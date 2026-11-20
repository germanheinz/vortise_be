package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.EmpresaDashboardObrasDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import com.vortise.gestion.domain.service.EstructuraRubros;
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
public class GetEmpresaDashboardObrasUseCase {
    private static final Set<String> CERRADAS = Set.of("RESUELTO", "RESUELTA", "LIBERADO");
    private static final String SUBCONTRATO = "SUBCONTRATO";
    private final EmpresaRepository empresas;
    private final ProyectoRepository proyectos;
    private final RestriccionObraRepository restricciones;
    private final GetPrevistoRealUseCase previstoReal;
    private final CurrentIdentity identity;

    public GetEmpresaDashboardObrasUseCase(EmpresaRepository empresas, ProyectoRepository proyectos,
            RestriccionObraRepository restricciones, GetPrevistoRealUseCase previstoReal, CurrentIdentity identity) {
        this.empresas = empresas; this.proyectos = proyectos; this.restricciones = restricciones;
        this.previstoReal = previstoReal; this.identity = identity;
    }

    public EmpresaDashboardObrasDto execute(Long empresaId) {
        identity.requireEmpresa(empresaId);
        if (empresas.findById(empresaId).isEmpty()) throw new RecursoNoEncontradoException("Empresa no encontrada");
        Map<Long, Long> activas = restricciones.findByProyectoEmpresaRelacionadaIdOrderByIdAsc(empresaId).stream()
            .filter(GetEmpresaDashboardObrasUseCase::activa)
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
        long activas = restricciones.findByProyectoId(proyectoId).stream().filter(GetEmpresaDashboardObrasUseCase::activa).count();
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

    private static Double sum(List<GetPrevistoRealUseCase.Activity> activities, Function<PrevistoRealCalculator.Metrics, Double> field) {
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
}
