package com.vortise.gestion.service.proyecto;

import com.vortise.gestion.application.dto.IndicadoresObraDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.rule.EstructuraRubros;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Carga;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Parametros;
import com.vortise.gestion.domain.rule.indicadores.CalculadoraIndicadores.Subrubro;
import com.vortise.gestion.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.ObraTarea;
import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.model.RubroObra;
import com.vortise.gestion.model.TipoNodoRubro;
import com.vortise.gestion.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.repository.ObraTareaRepository;
import com.vortise.gestion.repository.ProyectoRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the structure, schedule and progress loads of a project and computes its indicators (spec 021). */
@Service
@Transactional(readOnly = true)
public class IndicadoresObraService {
    static final String SUBCONTRATO = "SUBCONTRATO";

    private final ProyectoRepository proyectos;
    private final ObraTareaRepository tareas;
    private final CategoriaManoObraEmpresaRepository categorias;

    public IndicadoresObraService(ProyectoRepository proyectos, ObraTareaRepository tareas,
            CategoriaManoObraEmpresaRepository categorias) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.categorias = categorias;
    }

    public IndicadoresObraDto calcular(Long proyectoId) {
        Proyecto proyecto = proyectos.findById(proyectoId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        Parametros parametros = parametros(proyecto.getEmpresaRelacionada());
        List<RubroObra> rubros = proyecto.getRubros() == null ? List.of() : proyecto.getRubros();
        List<RubroObra> hojas = EstructuraRubros.hojas(rubros).stream().sorted(EstructuraRubros.ORDEN_NATURAL).toList();
        Set<Long> idsHojas = hojas.stream().map(RubroObra::getId).collect(Collectors.toSet());

        Map<Long, List<Carga>> cargasPorHoja = new HashMap<>();
        long sinVincular = 0;
        for (ObraTarea tarea : tareas.findByProyectoId(proyectoId)) {
            if (!idsHojas.contains(tarea.getRubroId())) {
                sinVincular++;
                continue;
            }
            cargasPorHoja.computeIfAbsent(tarea.getRubroId(), id -> new ArrayList<>()).add(new Carga(tarea.getId(),
                tarea.getFecha(), tarea.getDias(), tarea.getCantidadReal(), tarea.getHorasOficiales(),
                tarea.getHorasAyudantes(), tarea.isDesencofrado()));
        }

        List<Subrubro> subrubros = hojas.stream()
            .filter(hoja -> !SUBCONTRATO.equals(hoja.getTipoContratista()))
            .map(hoja -> subrubro(hoja, rubros, cargasPorHoja.getOrDefault(hoja.getId(), List.of())))
            .toList();
        var resultado = CalculadoraIndicadores.calcular(subrubros, parametros);
        return new IndicadoresObraDto(parametros, sinVincular, resultado.obra(), resultado.rubros());
    }

    /** Floor and subrubro name come from the structure variant: rubro → nivel → subrubro or rubro → subrubro → nivel. */
    private static Subrubro subrubro(RubroObra hoja, List<RubroObra> rubros, List<Carga> cargas) {
        RubroObra raiz = EstructuraRubros.raiz(hoja, rubros);
        List<RubroObra> intermedios = EstructuraRubros.intermedios(hoja);
        RubroObra nivel = hoja.getTipoNodo() == TipoNodoRubro.NIVEL ? hoja : ultimo(intermedios, TipoNodoRubro.NIVEL);
        RubroObra descripcion = hoja.getTipoNodo() == TipoNodoRubro.NIVEL ? ultimo(intermedios, TipoNodoRubro.SUBRUBRO) : hoja;
        String piso = nivel != null ? EstructuraRubros.nombreDe(nivel) : texto(hoja.getNivel());
        return new Subrubro(hoja.getId(), raiz == null ? null : raiz.getId(), raiz == null ? null : EstructuraRubros.nombreDe(raiz),
            nivel == null ? null : nivel.getId(), piso, EstructuraRubros.nombreDe(descripcion == null ? hoja : descripcion),
            hoja.getUnidad(), hoja.getCantidad(), hoja.getHorasOficialesPrevistas(), hoja.getHorasAyudantesPrevistas(),
            equipo(hoja, intermedios, raiz), cargas);
    }

    /** People of the schedule times its work fronts, from the closest node that defines them (leaf, groupers, rubro). */
    private static Integer equipo(RubroObra hoja, List<RubroObra> intermedios, RubroObra raiz) {
        List<RubroObra> candidatos = new ArrayList<>();
        candidatos.add(hoja);
        for (int i = intermedios.size() - 1; i >= 0; i--) candidatos.add(intermedios.get(i));
        if (raiz != null) candidatos.add(raiz);
        for (RubroObra nodo : candidatos) {
            if (nodo.getCantidadPersonas() != null) return nodo.getCantidadPersonas() * nodo.getFrentes();
        }
        return null;
    }

    private static RubroObra ultimo(List<RubroObra> nodos, TipoNodoRubro tipo) {
        for (int i = nodos.size() - 1; i >= 0; i--) if (nodos.get(i).getTipoNodo() == tipo) return nodos.get(i);
        return null;
    }

    private Parametros parametros(Empresa empresa) {
        List<CategoriaManoObraEmpresa> propias = empresa == null ? List.of() : categorias.findByEmpresaId(empresa.getId());
        var calendario = empresa == null ? null : empresa.getCalendarioLaboral();
        return new Parametros(costoHora(propias, "OFICIAL"), costoHora(propias, "AYUDANTE"),
            calendario == null ? null : calendario.getHorasLunesAViernes());
    }

    private static Double costoHora(List<CategoriaManoObraEmpresa> categorias, String nombre) {
        var coincidencias = categorias.stream().filter(c -> c.isHabilitada() && nombre.equals(c.getNombre())).toList();
        if (coincidencias.size() != 1) return null;
        Double valor = coincidencias.get(0).getCostoHora();
        return valor != null && Double.isFinite(valor) && valor >= 0 ? valor : null;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
