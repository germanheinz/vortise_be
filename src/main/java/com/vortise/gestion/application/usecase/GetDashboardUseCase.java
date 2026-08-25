/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.AvanceSistemaDto;
import com.vortise.gestion.application.dto.CostoPrecioEntregaDto;
import com.vortise.gestion.application.dto.DashboardDto;
import com.vortise.gestion.application.dto.DashboardKpisDto;
import com.vortise.gestion.application.dto.DocsSistemaDto;
import com.vortise.gestion.application.dto.EstadoDocDto;
import com.vortise.gestion.application.dto.SugerenciaMejoraDto;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.StatusTarea;
import com.vortise.gestion.domain.model.Tarea;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import com.vortise.gestion.domain.repository.IngresoRepository;
import com.vortise.gestion.domain.repository.PresupuestoLineaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.repository.TareaRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GetDashboardUseCase {
    private final ProyectoRepository proyectoRepository;
    private final TareaRepository tareaRepository;
    private final IngresoRepository ingresoRepository;
    private final GastoFijoRepository gastoFijoRepository;
    private final PresupuestoLineaRepository presupuestoLineaRepository;

    public DashboardDto execute(Long proyectoId) {
        Proyecto proyecto = this.proyectoRepository.findById(proyectoId).orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + proyectoId));
        List<Tarea> tareas = this.tareaRepository.findByEntregaProyectoId(proyectoId);
        DashboardKpisDto kpis = this.buildKpis(tareas, proyecto.getId());
        List<EstadoDocDto> estadoDoc = this.buildEstadoDocumentos(tareas);
        List<AvanceSistemaDto> avanceSistema = this.buildAvancePorSistema(tareas);
        List<DocsSistemaDto> docsSistema = this.buildDocsPorSistema(tareas);
        List<CostoPrecioEntregaDto> costoPorEntrega = this.buildCostoPorEntrega(tareas);
        List<SugerenciaMejoraDto> sugerencias = this.buildSugerencias(tareas);
        return new DashboardDto(kpis, estadoDoc, avanceSistema, docsSistema, costoPorEntrega, sugerencias);
    }

    private DashboardKpisDto buildKpis(List<Tarea> tareas, Long proyectoId) {
        int total = tareas.size();
        int ejecutadas = (int)tareas.stream().filter(t -> t.getStatus() == StatusTarea.EJECUTADO).count();
        int enProceso = (int)tareas.stream().filter(t -> t.getStatus() == StatusTarea.EN_PROCESO).count();
        int pendientes = (int)tareas.stream().filter(t -> t.getStatus() == StatusTarea.PENDIENTE).count();
        double avancePct = total == 0 ? 0.0 : (double)ejecutadas / (double)total * 100.0;
        double costoTotal = this.presupuestoLineaRepository.findByProyectoId(proyectoId).stream().mapToDouble(l -> {
            if (l.getM2() == null || l.getHhPorM2() == null || l.getPrecioPorHh() == null) {
                return 0.0;
            }
            return l.getM2() * l.getHhPorM2() * l.getPrecioPorHh();
        }).sum();
        double ingresoTotal = this.ingresoRepository.findByProyectoId(proyectoId).stream().mapToDouble(i -> i.getMonto()).sum();
        double costoIndTotal = this.gastoFijoRepository.findAll().stream().mapToDouble(g -> g.getMonto()).sum();
        double balance = ingresoTotal - costoTotal - costoIndTotal;
        return new DashboardKpisDto(total, ejecutadas, enProceso, pendientes, (double)Math.round(avancePct * 10.0) / 10.0, costoTotal, costoIndTotal, ingresoTotal, balance);
    }

    private List<EstadoDocDto> buildEstadoDocumentos(List<Tarea> tareas) {
        Map<StatusTarea, Long> counts = tareas.stream().collect(Collectors.groupingBy(Tarea::getStatus, Collectors.counting()));
        return List.of(new EstadoDocDto("PENDIENTE", counts.getOrDefault(StatusTarea.PENDIENTE, 0L).intValue()), new EstadoDocDto("EN_PROCESO", counts.getOrDefault(StatusTarea.EN_PROCESO, 0L).intValue()), new EstadoDocDto("EJECUTADO", counts.getOrDefault(StatusTarea.EJECUTADO, 0L).intValue()));
    }

    private List<AvanceSistemaDto> buildAvancePorSistema(List<Tarea> tareas) {
        Map<String, List<Tarea>> bySistema = tareas.stream().filter(t -> t.getSistema() != null).collect(Collectors.groupingBy(Tarea::getSistema));
        return bySistema.entrySet().stream().map(e -> {
            List<Tarea> ts = e.getValue();
            long ejecutadas = ts.stream().filter(t -> t.getStatus() == StatusTarea.EJECUTADO).count();
            double pct = ts.isEmpty() ? 0.0 : (double)ejecutadas / (double)ts.size() * 100.0;
            return new AvanceSistemaDto(e.getKey(), (double)Math.round(pct * 10.0) / 10.0);
        }).sorted(Comparator.comparingDouble(AvanceSistemaDto::avancePct).reversed()).toList();
    }

    private List<DocsSistemaDto> buildDocsPorSistema(List<Tarea> tareas) {
        Map<String, List<Tarea>> bySistema = tareas.stream().filter(t -> t.getSistema() != null).collect(Collectors.groupingBy(Tarea::getSistema));
        return bySistema.entrySet().stream().map(e -> {
            List<Tarea> ts = e.getValue();
            int pend = (int)ts.stream().filter(t -> t.getStatus() == StatusTarea.PENDIENTE).count();
            int enP = (int)ts.stream().filter(t -> t.getStatus() == StatusTarea.EN_PROCESO).count();
            int ejec = (int)ts.stream().filter(t -> t.getStatus() == StatusTarea.EJECUTADO).count();
            return new DocsSistemaDto(e.getKey(), pend, enP, ejec);
        }).toList();
    }

    private List<CostoPrecioEntregaDto> buildCostoPorEntrega(List<Tarea> tareas) {
        Map<String, List<Tarea>> byEntrega = tareas.stream().filter(t -> t.getEntrega() != null).collect(Collectors.groupingBy(t -> t.getEntrega().getNombre()));
        return byEntrega.entrySet().stream().map(e -> {
            double costo = e.getValue().stream().filter(t -> t.getCosto() != null).mapToDouble(Tarea::getCosto).sum();
            return new CostoPrecioEntregaDto(e.getKey(), costo, costo * 1.2);
        }).toList();
    }

    private List<SugerenciaMejoraDto> buildSugerencias(List<Tarea> tareas) {
        long sinCosto;
        long enProcesoCount;
        ArrayList<SugerenciaMejoraDto> sugerencias = new ArrayList<SugerenciaMejoraDto>();
        long pendientesCount = tareas.stream().filter(t -> t.getStatus() == StatusTarea.PENDIENTE).count();
        if ((double)pendientesCount > (double)tareas.size() * 0.5) {
            sugerencias.add(new SugerenciaMejoraDto("AVANCE", "M\u00e1s del 50% de las tareas est\u00e1n pendientes. Revisar cronograma.", "ALTA"));
        }
        if ((enProcesoCount = tareas.stream().filter(t -> t.getStatus() == StatusTarea.EN_PROCESO).count()) > 10L) {
            sugerencias.add(new SugerenciaMejoraDto("CARGA", enProcesoCount + " tareas en proceso simult\u00e1neamente. Considerar priorizaci\u00f3n.", "MEDIA"));
        }
        if ((sinCosto = tareas.stream().filter(t -> t.getCosto() == null || t.getCosto() == 0.0).count()) > 0L) {
            sugerencias.add(new SugerenciaMejoraDto("DATOS", sinCosto + " tareas sin costo asignado. Completar para mejor estimaci\u00f3n.", "BAJA"));
        }
        if (sugerencias.isEmpty()) {
            sugerencias.add(new SugerenciaMejoraDto("INFO", "El proyecto se encuentra al d\u00eda. \u00a1Buen trabajo!", "INFO"));
        }
        return sugerencias;
    }

    public GetDashboardUseCase(ProyectoRepository proyectoRepository, TareaRepository tareaRepository, IngresoRepository ingresoRepository, GastoFijoRepository gastoFijoRepository, PresupuestoLineaRepository presupuestoLineaRepository) {
        this.proyectoRepository = proyectoRepository;
        this.tareaRepository = tareaRepository;
        this.ingresoRepository = ingresoRepository;
        this.gastoFijoRepository = gastoFijoRepository;
        this.presupuestoLineaRepository = presupuestoLineaRepository;
    }
}
