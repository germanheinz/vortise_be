package com.vortise.gestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.service.RestriccionObraConsultaService.Filtro;
import com.vortise.gestion.domain.model.Proyecto;
import com.vortise.gestion.domain.repository.ProyectoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RestriccionObraConsultaServiceTest {
    private static final LocalDate CORTE = LocalDate.of(2026, 11, 20);
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final RestriccionService restricciones = mock(RestriccionService.class);
    private final RestriccionObraConsultaService consulta = new RestriccionObraConsultaService(restricciones, proyectos);

    @Test
    void filtraVencidasSinExcluirDelResumenLasAbiertasQueVencenHoy() {
        var obra = new Proyecto();
        obra.setNombre("Obra Norte");
        when(proyectos.findById(9L)).thenReturn(Optional.of(obra));
        when(restricciones.listarPorProyecto(9L)).thenReturn(List.of(
            restriccion(1L, CORTE.minusDays(1), null, " alta "),
            restriccion(2L, CORTE, null, "MEDIA"),
            restriccion(3L, CORTE.minusDays(2), CORTE, "ALTA")
        ));

        var resultado = consulta.execute(9L, CORTE, Filtro.VENCIDAS, "alta");

        assertThat(resultado.obra()).isEqualTo("Obra Norte");
        assertThat(resultado.total()).isEqualTo(1);
        assertThat(resultado.abiertas()).isEqualTo(2);
        assertThat(resultado.vencidas()).isEqualTo(1);
        assertThat(resultado.altaPrioridad()).isEqualTo(1);
        assertThat(resultado.detalle()).extracting(RestriccionObraConsultaService.Detalle::id).containsExactly(1L);
    }

    private static RestriccionDto restriccion(Long id, LocalDate limite, LocalDate entrega, String prioridad) {
        return new RestriccionDto(id, 9L, "Obra Norte", "Plano eléctrico", null, null, null,
            null, null, null, null, CORTE.minusDays(5), limite, entrega, null, prioridad, null, 0, null);
    }
}
