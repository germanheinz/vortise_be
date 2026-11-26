package com.vortise.gestion.service;

import com.vortise.gestion.application.helper.CalculoAnexoPrevistoReal;
import com.vortise.gestion.application.helper.PrevistoRealCalculator;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.domain.model.ObraTarea;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.ObraTareaRepository;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import com.vortise.gestion.domain.rule.EstructuraRubros;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrevistoRealService {
    private final ProyectoRepository proyectos;
    private final ObraTareaRepository tareas;
    private final CategoriaManoObraEmpresaRepository categorias;

    public PrevistoRealService(ProyectoRepository proyectos, ObraTareaRepository tareas,
            CategoriaManoObraEmpresaRepository categorias) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.categorias = categorias;
    }

    public record BaseAnexo(Integer dias, Integer personas, Double hhPrevistasFecha, Double hhPrevistasHecho, Double hhReales) { }
    public record Load(Long tareaId, PrevistoRealCalculator.Metrics indicadores, BaseAnexo baseAnexo) { }
    /** {@code conDesencofrado}: alguna carga de la actividad es de horas de desencofrado. */
    public record Activity(Long rubroId, String nombre, String unidad, Double cantidadReal,
            Double horasOficiales, Double horasAyudantes, PrevistoRealCalculator.Metrics indicadores,
            BaseAnexo baseAnexoAgregado, List<Load> cargas, boolean conDesencofrado) {
        public Activity(Long rubroId, String nombre, String unidad, Double cantidadReal,
                Double horasOficiales, Double horasAyudantes, PrevistoRealCalculator.Metrics indicadores, List<Load> cargas) {
            this(rubroId, nombre, unidad, cantidadReal, horasOficiales, horasAyudantes, indicadores, null, cargas, false);
        }
    }
    public record Summary(Double costoHoraOficial, Double costoHoraAyudante, Double horasJornada,
            long tareasSinVincular, List<Activity> actividades) { }

    private record DatosLaborales(Double costoHoraOficial, Double costoHoraAyudante, Double horasJornada) {
    }

    public Summary execute(Long proyectoId) {
        var proyecto = proyectos.findById(proyectoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        var datosLaborales = cargarDatosLaborales(proyecto.getEmpresaRelacionada());
        var cargas = tareas.findByProyectoId(proyectoId);
        var rubros = proyecto.getRubros() == null ? List.<RubroObra>of() : proyecto.getRubros();
        var tareasPorRubro = agruparPorRubro(cargas);
        var rubrosConocidos = rubros.stream().map(RubroObra::getId).collect(Collectors.toSet());

        var actividades = rubros.stream()
                .filter(rubro -> EstructuraRubros.esHoja(rubro, rubros)
                        || !tareasPorRubro.getOrDefault(rubro.getId(), List.of()).isEmpty())
                .map(rubro -> construirActividad(rubro, rubros,
                        tareasPorRubro.getOrDefault(rubro.getId(), List.of()), datosLaborales))
                .toList();
        long tareasSinVincular = cargas.stream().filter(tarea -> !rubrosConocidos.contains(tarea.getRubroId())).count();

        return new Summary(datosLaborales.costoHoraOficial(), datosLaborales.costoHoraAyudante(),
                datosLaborales.horasJornada(), tareasSinVincular, actividades);
    }

    private DatosLaborales cargarDatosLaborales(Empresa empresa) {
        Long empresaId = empresa == null ? null : empresa.getId();
        var categoriasEmpresa = empresaId == null
                ? List.<CategoriaManoObraEmpresa>of()
                : categorias.findByEmpresaId(empresaId);
        Double costoHoraOficial = costoHora(categoriasEmpresa, "OFICIAL");
        Double costoHoraAyudante = costoHora(categoriasEmpresa, "AYUDANTE");
        var calendario = empresa == null ? null : empresa.getCalendarioLaboral();
        Double horasJornada = calendario == null ? null : calendario.getHorasLunesAViernes();
        return new DatosLaborales(costoHoraOficial, costoHoraAyudante, horasJornada);
    }

    private static Map<Long, List<ObraTarea>> agruparPorRubro(List<ObraTarea> cargas) {
        Map<Long, List<ObraTarea>> tareasPorRubro = new HashMap<>();
        for (ObraTarea carga : cargas) {
            tareasPorRubro.computeIfAbsent(carga.getRubroId(), id -> new ArrayList<>()).add(carga);
        }
        return tareasPorRubro;
    }

    private static Activity construirActividad(RubroObra rubro, List<RubroObra> rubros,
            List<ObraTarea> tareasDelRubro, DatosLaborales datosLaborales) {
        Integer personas = dotacionPrevista(rubro, rubros);
        Double cantidadReal = sumarCantidades(tareasDelRubro);
        Double horasOficiales = sumarHoras(tareasDelRubro, ObraTarea::getHorasOficiales);
        Double horasAyudantes = sumarHoras(tareasDelRubro, ObraTarea::getHorasAyudantes);
        var cargas = tareasDelRubro.stream()
                .map(tarea -> construirCarga(rubro, tarea, datosLaborales, personas))
                .toList();
        var anexoAgregado = CalculoAnexoPrevistoReal.agregar(cargas);
        return new Activity(rubro.getId(), nombreActividad(rubro), rubro.getUnidad(), cantidadReal,
                horasOficiales, horasAyudantes,
                calcularIndicadores(rubro, cantidadReal, horasOficiales, horasAyudantes, datosLaborales),
                anexoAgregado, cargas, tareasDelRubro.stream().anyMatch(ObraTarea::isDesencofrado));
    }

    private static String nombreActividad(RubroObra rubro) {
        return rubro.getSubRubro() == null ? rubro.getRubro() : rubro.getSubRubro();
    }

    private static Load construirCarga(RubroObra rubro, ObraTarea tarea, DatosLaborales datosLaborales, Integer personas) {
        return new Load(tarea.getId(),
                calcularIndicadores(rubro, tarea.getCantidadReal(), tarea.getHorasOficiales(),
                        tarea.getHorasAyudantes(), datosLaborales),
                CalculoAnexoPrevistoReal.calcular(rubro, tarea, datosLaborales.horasJornada(), personas));
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

    private static Double costoHora(List<CategoriaManoObraEmpresa> categoriasEmpresa, String nombre) {
        var matches = categoriasEmpresa.stream().filter(c -> c.isHabilitada() && nombre.equals(c.getNombre())).toList();
        if (matches.size() != 1) return null;
        Double value = matches.get(0).getCostoHora();
        return value != null && Double.isFinite(value) && value >= 0 ? value : null;
    }

    /** Loads without a quantity are activities in process: they add nothing; without any quantity there is no data. */
    private static Double sumarCantidades(List<ObraTarea> tareasDelRubro) {
        var quantities = tareasDelRubro.stream().map(ObraTarea::getCantidadReal).filter(Objects::nonNull).toList();
        if (quantities.isEmpty() || quantities.stream().anyMatch(v -> !Double.isFinite(v) || v < 0)) return null;
        double total = quantities.stream().mapToDouble(Double::doubleValue).sum();
        return Double.isFinite(total) ? total : null;
    }

    private static Double sumarHoras(List<ObraTarea> tareasDelRubro, Function<ObraTarea, Double> horas) {
        if (tareasDelRubro.isEmpty() || tareasDelRubro.stream().map(horas).anyMatch(v -> v == null || !Double.isFinite(v) || v < 0)) return null;
        double total = tareasDelRubro.stream().map(horas).mapToDouble(Double::doubleValue).sum();
        return Double.isFinite(total) ? total : null;
    }

    private static PrevistoRealCalculator.Metrics calcularIndicadores(RubroObra rubro, Double cantidadReal,
            Double horasOficiales, Double horasAyudantes, DatosLaborales datosLaborales) {
        return PrevistoRealCalculator.calculate(rubro.getCantidad(), rubro.getHorasOficialesPrevistas(),
                rubro.getHorasAyudantesPrevistas(), cantidadReal, horasOficiales, horasAyudantes,
                datosLaborales.costoHoraOficial(), datosLaborales.costoHoraAyudante(), datosLaborales.horasJornada());
    }
}
