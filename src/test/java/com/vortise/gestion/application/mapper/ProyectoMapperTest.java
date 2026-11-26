package com.vortise.gestion.application.mapper;

import com.vortise.gestion.model.Proyecto;
import com.vortise.gestion.model.RubroObra;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProyectoMapperTest {
    @Test
    void returnsWorkOrderDeadlineAndRubrosInNumericStructureOrder() {
        Proyecto project = new Proyecto();
        project.setId(7L);
        project.setPedidosDeObraDias(30);
        project.setRubros(List.of(
            rubro(project, 3L, "5002", "Losas"),
            rubro(project, 2L, "5001", "Fundaciones"),
            rubro(project, 1L, "5000", "Hormigón")
        ));

        var result = ProyectoMapper.toDetailDto(project);

        assertEquals(30, result.pedidosDeObraDias());
        assertEquals(List.of("5000", "5001", "5002"), result.rubros().stream().map(item -> item.nRubro()).toList());
    }

    @Test
    void summaryLeavesDetailFieldsNullWithoutReadingCollections() {
        var proyecto = mock(Proyecto.class);

        var resumen = ProyectoMapper.toSummaryDto(proyecto);

        assertNull(resumen.plantas());
        assertNull(resumen.rubros());
        assertNull(resumen.subcontratistaIds());
        assertNull(resumen.contratos());
        verify(proyecto, never()).getPlantas();
        verify(proyecto, never()).getRubros();
        verify(proyecto, never()).getSubcontratistas();
        verify(proyecto, never()).getContratos();
    }

    @Test
    void detailReturnsEmptyCollectionsAndUsesLegacyCompanyName() {
        var proyecto = new Proyecto();
        proyecto.setEmpresa("Cliente histórico");
        proyecto.setPlantas(null);
        proyecto.setRubros(null);
        proyecto.setContratos(null);

        var detalle = ProyectoMapper.toDetailDto(proyecto);
        var resumen = ProyectoMapper.toSummaryDto(proyecto);

        assertEquals(List.of(), detalle.plantas());
        assertEquals(List.of(), detalle.rubros());
        assertEquals(List.of(), detalle.subcontratistaIds());
        assertEquals(List.of(), detalle.contratos());
        assertNull(detalle.empresaId());
        assertEquals("Cliente histórico", detalle.empresaNombre());
        assertEquals(detalle.empresaNombre(), resumen.empresaNombre());
    }

    private RubroObra rubro(Proyecto project, Long id, String code, String name) {
        RubroObra rubro = new RubroObra();
        rubro.setId(id);
        rubro.setProyecto(project);
        rubro.setnRubro(code);
        rubro.setRubro("Hormigón");
        rubro.setSubRubro(name);
        return rubro;
    }
}
