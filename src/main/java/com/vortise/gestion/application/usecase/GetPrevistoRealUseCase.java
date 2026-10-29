package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetPrevistoRealUseCase {
    private final ProyectoRepository projects;
    private final ObraTareaRepository tasks;
    private final CategoriaManoObraEmpresaRepository categories;
    private final CalendarioLaboralEmpresaRepository calendars;

    public GetPrevistoRealUseCase(ProyectoRepository projects, ObraTareaRepository tasks,
            CategoriaManoObraEmpresaRepository categories, CalendarioLaboralEmpresaRepository calendars) {
        this.projects = projects; this.tasks = tasks; this.categories = categories; this.calendars = calendars;
    }

    public record Load(Long tareaId, PrevistoRealCalculator.Metrics indicadores) { }
    public record Activity(Long rubroId, String nombre, String unidad, Double cantidadReal,
            Double horasOficiales, Double horasAyudantes, PrevistoRealCalculator.Metrics indicadores, List<Load> cargas) { }
    public record Summary(Double costoHoraOficial, Double costoHoraAyudante, Double horasJornada,
            long tareasSinVincular, List<Activity> actividades) { }

    public Summary execute(Long projectId) {
        var project = projects.findById(projectId).orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        var companyId = project.getEmpresaRelacionada() == null ? null : project.getEmpresaRelacionada().getId();
        var cats = companyId == null ? List.<CategoriaManoObraEmpresa>of() : categories.findByEmpresaId(companyId);
        Double of = cost(cats, "OFICIAL"), ay = cost(cats, "AYUDANTE");
        Double day = companyId == null ? null : calendars.findByEmpresaId(companyId).map(CalendarioLaboralEmpresa::getHorasLunesAViernes).orElse(null);
        var loads = tasks.findByProyectoId(projectId);
        var rubros = project.getRubros() == null ? List.<RubroObra>of() : project.getRubros();
        var byRubro = new java.util.HashMap<Long, java.util.List<ObraTarea>>();
        for (var load : loads) byRubro.computeIfAbsent(load.getRubroId(), key -> new java.util.ArrayList<>()).add(load);
        var knownIds = rubros.stream().map(RubroObra::getId).collect(java.util.stream.Collectors.toSet());
        var activities = rubros.stream().map(r -> {
            var matching = byRubro.getOrDefault(r.getId(), List.of());
            Double q = sum(matching, ObraTarea::getCantidadReal), hOf = sum(matching, ObraTarea::getHorasOficiales), hAy = sum(matching, ObraTarea::getHorasAyudantes);
            return new Activity(r.getId(), r.getSubRubro() == null ? r.getRubro() : r.getSubRubro(), r.getUnidad(), q, hOf, hAy,
                metrics(r, q, hOf, hAy, of, ay, day), matching.stream().map(t -> new Load(t.getId(),
                    metrics(r, t.getCantidadReal(), t.getHorasOficiales(), t.getHorasAyudantes(), of, ay, day))).toList());
        }).toList();
        long unlinked = loads.stream().filter(t -> !knownIds.contains(t.getRubroId())).count();
        return new Summary(of, ay, day, unlinked, activities);
    }

    private static Double cost(List<CategoriaManoObraEmpresa> categories, String name) {
        var matches = categories.stream().filter(c -> c.isHabilitada() && name.equals(c.getNombre())).toList();
        if (matches.size() != 1) return null;
        Double value = matches.get(0).getCostoHora();
        return value != null && Double.isFinite(value) && value >= 0 ? value : null;
    }

    private static Double sum(List<ObraTarea> tasks, Function<ObraTarea, Double> field) {
        if (tasks.isEmpty() || tasks.stream().map(field).anyMatch(v -> v == null || !Double.isFinite(v) || v < 0)) return null;
        double total = tasks.stream().map(field).mapToDouble(Double::doubleValue).sum();
        return Double.isFinite(total) ? total : null;
    }

    private static PrevistoRealCalculator.Metrics metrics(RubroObra r, Double q, Double of, Double ay, Double costOf, Double costAy, Double day) {
        return PrevistoRealCalculator.calculate(r.getCantidad(), r.getHorasOficialesPrevistas(), r.getHorasAyudantesPrevistas(), q, of, ay, costOf, costAy, day);
    }
}
