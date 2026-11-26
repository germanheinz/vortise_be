package com.vortise.gestion.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vortise.gestion.application.dto.RestriccionDto;
import com.vortise.gestion.application.helper.RestriccionResumenGerencial;
import com.vortise.gestion.application.helper.RestriccionResumenGerencial.Filtro;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RestriccionResumenGerencialTest {
    private static final LocalDate CORTE = LocalDate.of(2026, 11, 20);

    @Test
    void filtraVencidasSinExcluirDelResumenLasAbiertasQueVencenHoy() {
        var restricciones = List.of(
            restriccion(1L, CORTE.minusDays(1), null, " alta "),
            restriccion(2L, CORTE, null, "MEDIA"),
            restriccion(3L, CORTE.minusDays(2), CORTE, "ALTA")
        );
        var resultado = RestriccionResumenGerencial.calcular(9L, "Obra Norte", CORTE, Filtro.VENCIDAS, "alta", restricciones);

        assertThat(resultado.obra()).isEqualTo("Obra Norte");
        assertThat(resultado.total()).isEqualTo(1);
        assertThat(resultado.abiertas()).isEqualTo(2);
        assertThat(resultado.vencidas()).isEqualTo(1);
        assertThat(resultado.altaPrioridad()).isEqualTo(1);
        assertThat(resultado.detalle()).extracting(RestriccionResumenGerencial.Detalle::id).containsExactly(1L);
    }

    private static RestriccionDto restriccion(Long id, LocalDate limite, LocalDate entrega, String prioridad) {
        return new RestriccionDto(id, 9L, "Obra Norte", "Plano eléctrico", null, null, null,
            null, null, null, null, CORTE.minusDays(5), limite, entrega, null, prioridad, null, 0, null);
    }
}
