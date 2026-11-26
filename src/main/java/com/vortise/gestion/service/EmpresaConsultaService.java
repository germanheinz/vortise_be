package com.vortise.gestion.service;

import com.vortise.gestion.application.dto.EmpresaDashboardObrasDto;
import com.vortise.gestion.application.dto.EmpresaResumenDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.helper.PrevistoRealCalculator;
import com.vortise.gestion.application.mapper.ObraTareaMapper;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.model.RestriccionObra;
import com.vortise.gestion.model.RubroObra;
import com.vortise.gestion.repository.EmpresaConteoProjection;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.ObraTareaRepository;
import com.vortise.gestion.repository.ProyectoRepository;
import com.vortise.gestion.repository.RegistroHorasRepository;
import com.vortise.gestion.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.rule.EstructuraRubros;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmpresaConsultaService {
    private static final double CUMPLIDA = 100.0;
    private static final Set<String> CERRADAS = Set.of("RESUELTO", "RESUELTA", "LIBERADO");
    private static final String SUBCONTRATO = "SUBCONTRATO";
    private final EmpresaRepository empresas;
    private final ProyectoRepository proyectos;
    private final RestriccionObraRepository restricciones;
    private final PrevistoRealService previstoReal;
    private final CurrentIdentity identity;
    private final RegistroHorasRepository horas;
    private final ObraTareaRepository tareas;

    public EmpresaConsultaService(EmpresaRepository empresas, ProyectoRepository proyectos,
            RestriccionObraRepository restricciones, PrevistoRealService previstoReal, CurrentIdentity identity,
            RegistroHorasRepository horas, ObraTareaRepository tareas) {
        this.empresas = empresas; this.proyectos = proyectos; this.restricciones = restricciones;
        this.previstoReal = previstoReal; this.identity = identity;
        this.horas = horas;
        this.tareas = tareas;
    }

    public EmpresaDashboardObrasDto dashboardObras(Long empresaId) {
        validarEmpresa(empresaId);
        Map<Long, Long> activas = restricciones.findByProyectoEmpresaRelacionadaIdOrderByIdAsc(empresaId).stream()
            .filter(EmpresaConsultaService::activa)
            .collect(Collectors.groupingBy(r -> r.getProyecto().getId(), Collectors.counting()));
        var obras = proyectos.findByEmpresaRelacionadaId(empresaId).stream()
            .sorted(Comparator.comparing(Proyecto::getId))
            .map(obra -> obra(obra, activas.getOrDefault(obra.getId(), 0L))).toList();
        return new EmpresaDashboardObrasDto(empresaId, obras);
    }

    /** Live deviation snapshot of one obra, with the same rules as the company dashboard. */
    public EmpresaDashboardObrasDto.Obra obra(Long proyectoId) {
        var proyecto = proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        if (proyecto.getEmpresaRelacionada() == null) throw new RecursoNoEncontradoException("Obra no encontrada");
        identity.requireEmpresa(proyecto.getEmpresaRelacionada().getId());
        long activas = restricciones.findByProyectoId(proyectoId).stream().filter(EmpresaConsultaService::activa).count();
        return obra(proyecto, activas);
    }

    /** An empty or unknown state counts as active, like the restrictions board treats it as pending. */
    static boolean activa(RestriccionObra restriccion) {
        String estado = restriccion.getEstado();
        return estado == null || !CERRADAS.contains(estado.trim().toUpperCase(Locale.ROOT));
    }

    private EmpresaDashboardObrasDto.Obra obra(Proyecto obra, long restriccionesActivas) {
        var rubros = obra.getRubros() == null ? List.<RubroObra>of() : obra.getRubros();
        var propias = rubros.stream().filter(r -> EstructuraRubros.esHoja(r, rubros) && !SUBCONTRATO.equals(r.getTipoContratista()))
            .map(RubroObra::getId).collect(Collectors.toSet());
        var resumen = previstoReal.execute(obra.getId());
        var actividades = resumen.actividades().stream().filter(a -> propias.contains(a.rubroId())).toList();
        Double actual = sum(actividades, PrevistoRealCalculator.Metrics::desvio);
        Double proyectado = sum(actividades, PrevistoRealCalculator.Metrics::proyeccion);
        Double costo = costoPrevisto(rubros, propias, resumen.costoHoraOficial(), resumen.costoHoraAyudante());
        int conDatos = (int) actividades.stream().filter(a -> a.indicadores().desvio() != null).count();
        return new EmpresaDashboardObrasDto.Obra(obra.getId(), obra.getNombre(), obra.getNumeroProyecto(), actual, proyectado,
            porcentaje(actual, costo), porcentaje(proyectado, costo), costo, propias.size(), conDatos, restriccionesActivas);
    }

    private static Double sum(List<PrevistoRealService.Activity> activities, Function<PrevistoRealCalculator.Metrics, Double> field) {
        var values = activities.stream().map(a -> field.apply(a.indicadores())).filter(v -> v != null && Double.isFinite(v)).toList();
        if (values.isEmpty()) return null;
        double total = values.stream().mapToDouble(Double::doubleValue).sum();
        return Double.isFinite(total) ? total : null;
    }

    private static Double costoPrevisto(List<RubroObra> rubros, Set<Long> propias, Double costoOficial, Double costoAyudante) {
        if (costoOficial == null || costoAyudante == null) return null;
        double total = 0;
        for (var rubro : rubros) {
            if (!propias.contains(rubro.getId())) continue;
            Double oficiales = rubro.getHorasOficialesPrevistas(), ayudantes = rubro.getHorasAyudantesPrevistas();
            if (oficiales == null || ayudantes == null || !Double.isFinite(oficiales) || !Double.isFinite(ayudantes)) continue;
            total += oficiales * costoOficial + ayudantes * costoAyudante;
        }
        return total > 0 && Double.isFinite(total) ? total : null;
    }

    private static Double porcentaje(Double monto, Double costo) {
        if (monto == null || costo == null || costo <= 0) return null;
        double value = monto / costo * 100;
        return Double.isFinite(value) ? value : null;
    }

    public EmpresaResumenDto resumen(Long empresaId) {
        validarEmpresa(empresaId);
        Set<Long> ids = Set.of(empresaId);
        Double totalHoras = horas.sumHorasPorEmpresaRelacionada(empresaId);
        var vinculadas = tareas.findConActividadByEmpresaRelacionadaId(empresaId);
        // Same rule the task list shows: the persisted percentage is ignored when the task has an activity.
        long cumplidas = vinculadas.stream().map(ObraTareaMapper::toDto)
            .filter(dto -> dto.porcentajeCumplimiento() != null && dto.porcentajeCumplimiento() >= CUMPLIDA).count();
        return new EmpresaResumenDto(empresaId, count(proyectos.contarPorEmpresaRelacionada(ids), empresaId),
            count(restricciones.contarPorEmpresaRelacionada(ids), empresaId), totalHoras == null ? 0.0 : totalHoras,
            vinculadas.size(), cumplidas);
    }

    private static long count(List<EmpresaConteoProjection> rows, Long empresaId) {
        return rows.stream().filter(row -> empresaId.equals(row.getEmpresaId())).mapToLong(EmpresaConteoProjection::getCantidad).sum();
    }

    private void validarEmpresa(Long empresaId) {
        identity.requireEmpresa(empresaId);
        if (empresas.findById(empresaId).isEmpty()) throw new RecursoNoEncontradoException("Empresa no encontrada");
    }
}
