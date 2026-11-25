package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.*;
import com.vortise.gestion.domain.service.EstructuraRubros;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    public record BaseAnexo(Integer dias, Integer personas, Double hhPrevistasFecha, Double hhPrevistasHecho, Double hhReales) { }
    public record Load(Long tareaId, PrevistoRealCalculator.Metrics indicadores, BaseAnexo baseAnexo) { }
    /** {@code conDesencofrado}: alguna carga de la actividad es de horas de desencofrado. */
    public record Activity(Long rubroId, String nombre, String unidad, Double cantidadReal,
            Double horasOficiales, Double horasAyudantes, PrevistoRealCalculator.Metrics indicadores, BaseAnexo baseAnexoAgregado, List<Load> cargas, boolean conDesencofrado) {
        public Activity(Long rubroId, String nombre, String unidad, Double cantidadReal,
                Double horasOficiales, Double horasAyudantes, PrevistoRealCalculator.Metrics indicadores, List<Load> cargas) {
            this(rubroId, nombre, unidad, cantidadReal, horasOficiales, horasAyudantes, indicadores, null, cargas, false);
        }
    }
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
        var activities = rubros.stream().filter(r -> (EstructuraRubros.esHoja(r, rubros) || !byRubro.getOrDefault(r.getId(), List.of()).isEmpty())).map(r -> {
            var matching = byRubro.getOrDefault(r.getId(), List.of());
            Integer personas = dotacionPrevista(r, rubros);
            Double q = sumQuantity(matching), hOf = sum(matching, ObraTarea::getHorasOficiales), hAy = sum(matching, ObraTarea::getHorasAyudantes);
            var cargas = matching.stream().map(t -> new Load(t.getId(),
                metrics(r, t.getCantidadReal(), t.getHorasOficiales(), t.getHorasAyudantes(), of, ay, day), baseAnexo(r, t, day, personas))).toList();
            var baseAnexoAgr = agregadoBaseAnexo(cargas);
            return new Activity(r.getId(), r.getSubRubro() == null ? r.getRubro() : r.getSubRubro(), r.getUnidad(), q, hOf, hAy,
                metrics(r, q, hOf, hAy, of, ay, day), baseAnexoAgr, cargas,
                matching.stream().anyMatch(ObraTarea::isDesencofrado));
        }).toList();
        long unlinked = loads.stream().filter(t -> !knownIds.contains(t.getRubroId())).count();
        return new Summary(of, ay, day, unlinked, activities);
    }

    private static Integer dotacionPrevista(RubroObra rubro, List<RubroObra> rubros) {
        if (rubro.getCantidadPersonas() != null) return rubro.getCantidadPersonas();
        var intermedios = EstructuraRubros.intermedios(rubro);
        for (int i = intermedios.size() - 1; i >= 0; i--) {
            Integer personas = intermedios.get(i).getCantidadPersonas();
            if (personas != null) return personas;
        }
        var raiz = EstructuraRubros.raiz(rubro, rubros);
        return raiz == null ? null : raiz.getCantidadPersonas();
    }

    private static BaseAnexo agregadoBaseAnexo(List<Load> loads) {
        var conAnexo = loads.stream().filter(l -> l.baseAnexo() != null).toList();
        if (conAnexo.isEmpty()) return null;
        Double hf = null, hg = null, hr = null;
        Integer dias = null, personas = null;
        for (var load : conAnexo) {
            var ba = load.baseAnexo();
            hf = sumar(hf, ba.hhPrevistasFecha());
            hg = sumar(hg, ba.hhPrevistasHecho());
            hr = sumar(hr, ba.hhReales());
        }
        return new BaseAnexo(dias, personas, hf, hg, hr);
    }

    private static Double sumar(Double a, Double b) {
        if (a == null || !Double.isFinite(a)) return b;
        if (b == null || !Double.isFinite(b)) return a;
        double suma = a + b;
        return Double.isFinite(suma) ? suma : null;
    }

    private static BaseAnexo baseAnexo(RubroObra rubro, ObraTarea tarea, Double jornada, Integer personas) {
        // No excluir SUBCONTRATOS: permitir que usen indicadores de anexo
        // Calcular días transcurridos desde la fecha de inicio del rubro hasta hoy
        Integer diasTranscurridos = null;
        if (rubro.getFechaInicioPlanificada() != null) {
            LocalDate hoy = LocalDate.now();
            long dias = ChronoUnit.DAYS.between(rubro.getFechaInicioPlanificada(), hoy);
            diasTranscurridos = dias > 0 ? (int) dias : null;
        }
        
        Double hf = personas != null && personas > 0 && diasTranscurridos != null && diasTranscurridos >= 0 && noNegativo(jornada) && jornada > 0
            ? finito(personas * jornada * diasTranscurridos) : null;
        Double cantidad = tarea.getCantidadReal();
        Double cantidadPrevista = rubro.getCantidad();
        Double ofPrevistas = rubro.getHorasOficialesPrevistas();
        Double ayPrevistas = rubro.getHorasAyudantesPrevistas();
        Double hg = noNegativo(cantidad) && noNegativo(cantidadPrevista) && cantidadPrevista > 0 && noNegativo(ofPrevistas) && noNegativo(ayPrevistas)
            ? finito(cantidad * ((ofPrevistas + ayPrevistas) / cantidadPrevista)) : null;
        Double of = tarea.getHorasOficiales(), ay = tarea.getHorasAyudantes();
        Double hr = noNegativo(of) && noNegativo(ay) ? finito(of + ay) : null;
        
        // Si hf es null (sin personas/jornada), usar horas previstas del rubro como hf
        if (hf == null && noNegativo(ofPrevistas) && noNegativo(ayPrevistas)) {
            hf = finito(ofPrevistas + ayPrevistas);
        }
        
        return new BaseAnexo(diasTranscurridos, personas, hf, hg, hr);
    }

    private static boolean noNegativo(Double valor) {
        return valor != null && Double.isFinite(valor) && valor >= 0;
    }

    private static Double finito(double valor) {
        return Double.isFinite(valor) ? valor : null;
    }

    private static Double cost(List<CategoriaManoObraEmpresa> categories, String name) {
        var matches = categories.stream().filter(c -> c.isHabilitada() && name.equals(c.getNombre())).toList();
        if (matches.size() != 1) return null;
        Double value = matches.get(0).getCostoHora();
        return value != null && Double.isFinite(value) && value >= 0 ? value : null;
    }

    /** Loads without a quantity are activities in process: they add nothing; without any quantity there is no data. */
    private static Double sumQuantity(List<ObraTarea> tasks) {
        var quantities = tasks.stream().map(ObraTarea::getCantidadReal).filter(java.util.Objects::nonNull).toList();
        if (quantities.isEmpty() || quantities.stream().anyMatch(v -> !Double.isFinite(v) || v < 0)) return null;
        double total = quantities.stream().mapToDouble(Double::doubleValue).sum();
        return Double.isFinite(total) ? total : null;
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
