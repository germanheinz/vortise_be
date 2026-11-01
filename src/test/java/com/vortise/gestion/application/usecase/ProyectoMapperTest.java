package com.vortise.gestion.application.usecase;

import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.model.RubroObra;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
