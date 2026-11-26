package com.vortise.gestion.service.empresa;

import com.vortise.gestion.service.proyecto.IndicadoresObraService;
import com.vortise.gestion.application.dto.EmpresaDashboardObrasDto;
import com.vortise.gestion.application.dto.EmpresaResumenDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores;
import com.vortise.gestion.application.mapper.ObraTareaMapper;
import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.model.RestriccionObra;
import com.vortise.gestion.repository.EmpresaConteoProjection;
import com.vortise.gestion.repository.EmpresaRepository;
import com.vortise.gestion.repository.ObraTareaRepository;
import com.vortise.gestion.repository.ProyectoRepository;
import com.vortise.gestion.repository.RegistroHorasRepository;
import com.vortise.gestion.repository.RestriccionObraRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmpresaConsultaService {
    private static final double CUMPLIDA = 100.0;
    private static final Set<String> CERRADAS = Set.of("RESUELTO", "RESUELTA", "LIBERADO");
    private final EmpresaRepository empresas;
    private final ProyectoRepository proyectos;
    private final RestriccionObraRepository restricciones;
    private final IndicadoresObraService indicadores;
    private final CurrentIdentity identity;
    private final RegistroHorasRepository horas;
    private final ObraTareaRepository tareas;

    public EmpresaConsultaService(EmpresaRepository empresas, ProyectoRepository proyectos,
            RestriccionObraRepository restricciones, IndicadoresObraService indicadores, CurrentIdentity identity,
            RegistroHorasRepository horas, ObraTareaRepository tareas) {
        this.empresas = empresas; this.proyectos = proyectos; this.restricciones = restricciones;
        this.indicadores = indicadores; this.identity = identity;
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

    /** Same figures as the project indicators (spec 021, CI-07): own labour only, percentages ×100. */
    private EmpresaDashboardObrasDto.Obra obra(Proyecto obra, long restriccionesActivas) {
        var indicadores = this.indicadores.calcular(obra.getId());
        var total = indicadores.obra();
        var subrubros = indicadores.rubros().stream().flatMap(r -> r.pisos().stream()).flatMap(p -> p.subrubros().stream()).toList();
        int conDatos = (int) subrubros.stream().filter(CalculadoraIndicadores.ResultadoSubrubro::conAvance).count();
        return new EmpresaDashboardObrasDto.Obra(obra.getId(), obra.getNombre(), obra.getNumeroProyecto(), total.desvio(),
            total.proyeccionTotal(), porcentaje(total.desvioPorcentaje()), porcentaje(total.porcentajeTotal()),
            total.presupuesto(), subrubros.size(), conDatos, restriccionesActivas);
    }

    private static Double porcentaje(Double fraccion) {
        return fraccion == null ? null : fraccion * 100;
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
