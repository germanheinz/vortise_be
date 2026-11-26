package com.vortise.gestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vortise.gestion.model.CalendarioLaboralEmpresa;
import com.vortise.gestion.model.CategoriaManoObraEmpresa;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.ObraTarea;
import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.model.RubroObra;
import com.vortise.gestion.model.TipoNodoRubro;
import com.vortise.gestion.model.VarianteEstructura;
import com.vortise.gestion.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.repository.ObraTareaRepository;
import com.vortise.gestion.repository.ProyectoRepository;
import com.vortise.gestion.service.proyecto.IndicadoresObraService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** How the structure, schedule and loads of a project become the inputs of the indicators (spec 021). */
class IndicadoresObraServiceTest {
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final ObraTareaRepository tareas = mock(ObraTareaRepository.class);
    private final CategoriaManoObraEmpresaRepository categorias = mock(CategoriaManoObraEmpresaRepository.class);
    private final IndicadoresObraService service = new IndicadoresObraService(proyectos, tareas, categorias);
    private final Proyecto proyecto = new Proyecto();
    private final List<RubroObra> rubros = new ArrayList<>();

    IndicadoresObraServiceTest() {
        Empresa empresa = mock(Empresa.class);
        CalendarioLaboralEmpresa calendario = mock(CalendarioLaboralEmpresa.class);
        when(empresa.getId()).thenReturn(1L);
        when(empresa.getCalendarioLaboral()).thenReturn(calendario);
        when(calendario.getHorasLunesAViernes()).thenReturn(8.0);
        when(categorias.findByEmpresaId(1L)).thenReturn(List.of(new CategoriaManoObraEmpresa(empresa, "OFICIAL", 5000.0, true),
            new CategoriaManoObraEmpresa(empresa, "AYUDANTE", 4000.0, true)));
        proyecto.setId(9L);
        proyecto.setEmpresaRelacionada(empresa);
        proyecto.setRubros(rubros);
        when(proyectos.findById(9L)).thenReturn(Optional.of(proyecto));
        when(tareas.findByProyectoId(any())).thenReturn(List.of());
    }

    private RubroObra nodo(long id, String codigo, TipoNodoRubro tipo, RubroObra parent, String nombre) {
        RubroObra rubro = new RubroObra();
        rubro.setId(id);
        rubro.setProyecto(proyecto);
        rubro.setnRubro(codigo);
        rubro.setTipoNodo(tipo);
        rubro.setParent(parent);
        rubro.setNombre(nombre);
        rubros.add(rubro);
        return rubro;
    }

    private RubroObra hoja(long id, String codigo, TipoNodoRubro tipo, RubroObra parent, String nombre, double cantidad, double rendimiento) {
        RubroObra hoja = nodo(id, codigo, tipo, parent, nombre);
        hoja.setUnidad("m3");
        hoja.setCantidad(cantidad);
        hoja.setHorasOficialesPrevistas(cantidad * rendimiento);
        hoja.setHorasAyudantesPrevistas(cantidad * rendimiento);
        return hoja;
    }

    private ObraTarea carga(long id, RubroObra actividad, int dias, double cantidad, double oficiales, double ayudantes) {
        ObraTarea tarea = new ObraTarea();
        tarea.setId(id);
        tarea.setActividad(actividad);
        tarea.setDias(dias);
        tarea.setCantidadReal(cantidad);
        tarea.setHorasOficiales(oficiales);
        tarea.setHorasAyudantes(ayudantes);
        return tarea;
    }

    @Test void rubroNivelSubrubroGroupsLeavesByFloorAndInheritsPeopleTimesFronts() {
        RubroObra hormigon = nodo(1, "1", TipoNodoRubro.RUBRO, null, "Hormigón");
        hormigon.setVarianteEstructura(VarianteEstructura.RUBRO_NIVEL_SUBRUBRO);
        RubroObra subsuelo2 = nodo(2, "1.1", TipoNodoRubro.NIVEL, hormigon, "Subsuelo 2");
        subsuelo2.setCantidadPersonas(1);
        subsuelo2.setFrentes(2);
        RubroObra platea = hoja(3, "1.1.1", TipoNodoRubro.SUBRUBRO, subsuelo2, "Platea", 20, 2.5);
        RubroObra subsuelo1 = nodo(4, "1.2", TipoNodoRubro.NIVEL, hormigon, "Subsuelo 1");
        hoja(5, "1.2.1", TipoNodoRubro.SUBRUBRO, subsuelo1, "Platea", 20, 2.5);
        when(tareas.findByProyectoId(9L)).thenReturn(List.of(carga(1, platea, 3, 10, 40, 40), carga(2, hormigon, 1, 1, 1, 1)));

        var resultado = service.calcular(9L);

        var rubro = resultado.rubros().get(0);
        assertEquals("Hormigón", rubro.nombre());
        assertEquals(List.of("Subsuelo 2", "Subsuelo 1"), rubro.pisos().stream().map(p -> p.nombre()).toList());
        assertEquals(2L, rubro.pisos().get(0).nivelId());
        var plateaSubsuelo2 = rubro.pisos().get(0).subrubros().get(0);
        assertEquals("Platea", plateaSubsuelo2.nombre());
        assertEquals(2, plateaSubsuelo2.presupuesto().personas(), "1 persona × 2 frentes");
        assertEquals(48, plateaSubsuelo2.hhPrevistasFecha(), 1e-9);
        assertEquals(-270_000, rubro.pisos().get(1).subrubros().get(0).proyeccion(), 1e-6);
        assertEquals(1, resultado.tareasSinVincular(), "a load on a grouping node is not linked to a leaf");
        assertEquals(5000.0, resultado.parametros().costoHoraOficial());
    }

    @Test void rubroSubrubroNivelTakesTheFloorFromTheLeafAndTheNameFromItsSubrubro() {
        RubroObra mamposteria = nodo(1, "2", TipoNodoRubro.RUBRO, null, "Mampostería");
        mamposteria.setVarianteEstructura(VarianteEstructura.RUBRO_SUBRUBRO_NIVEL);
        RubroObra tabique = nodo(2, "2.1", TipoNodoRubro.SUBRUBRO, mamposteria, "Tabique");
        hoja(3, "2.1.1", TipoNodoRubro.NIVEL, tabique, "SS1", 15, 1);
        hoja(4, "2.1.2", TipoNodoRubro.NIVEL, tabique, "SS2", 15, 1);

        var rubro = service.calcular(9L).rubros().get(0);

        assertEquals(List.of("SS1", "SS2"), rubro.pisos().stream().map(p -> p.nombre()).toList());
        assertEquals("Tabique", rubro.pisos().get(0).subrubros().get(0).nombre());
        assertEquals(3L, rubro.pisos().get(0).nivelId());
    }

    @Test void subcontractedLeavesAreLeftOut() {
        RubroObra rubro = nodo(1, "1", TipoNodoRubro.RUBRO, null, "Hormigón");
        rubro.setVarianteEstructura(VarianteEstructura.RUBRO_SUBRUBRO);
        hoja(2, "1.1", TipoNodoRubro.SUBRUBRO, rubro, "Propia", 10, 1);
        hoja(3, "1.2", TipoNodoRubro.SUBRUBRO, rubro, "Subcontratada", 10, 1).setTipoContratista("SUBCONTRATO");

        var resultado = service.calcular(9L);

        var piso = resultado.rubros().get(0).pisos().get(0);
        assertNull(piso.nombre(), "a rubro without floors forms a single unnamed group");
        assertEquals(List.of("Propia"), piso.subrubros().stream().map(s -> s.nombre()).toList());
        assertEquals(90_000, resultado.obra().presupuesto(), 1e-6);
    }
}
