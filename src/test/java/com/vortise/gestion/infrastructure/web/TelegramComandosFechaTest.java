package com.vortise.gestion.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

class TelegramComandosFechaTest {
    @Test
    void aceptaAmbosFormatosYMuestraDiaMesAnio() {
        assertThat(TelegramComandos.fechaDe("27/11/2099")).isEqualTo(LocalDate.of(2099, 11, 27));
        assertThat(TelegramComandos.fechaDe("2099-11-27")).isEqualTo(LocalDate.of(2099, 11, 27));
        assertThat(TelegramComandos.FECHA.format(LocalDate.of(2099, 1, 5))).isEqualTo("5/1/2099");
    }

    @Test
    void rechazaFechasInexistentes() {
        assertThatThrownBy(() -> TelegramComandos.fechaDe("31/02/2099")).isInstanceOf(DateTimeParseException.class);
        assertThatThrownBy(() -> TelegramComandos.fechaDe("mañana")).isInstanceOf(DateTimeParseException.class);
    }
}
