package com.vortise.gestion.domain.model;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VinculoTelegramTest {
    private static final Instant AHORA = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void requiereCodigoDeUnSoloUsoYConfirmacionDelMismoChat() {
        var vinculo = new VinculoTelegram(7L);
        vinculo.iniciar("hash", AHORA.plusSeconds(60));

        assertFalse(vinculo.confirmar(123L, AHORA));
        assertTrue(vinculo.aceptar("hash", 123L, AHORA));
        assertFalse(vinculo.aceptar("hash", 456L, AHORA));
        assertNull(vinculo.getChatConfirmado());
        assertFalse(vinculo.confirmar(456L, AHORA));
        assertTrue(vinculo.confirmar(123L, AHORA));
        assertEquals(123L, vinculo.getChatConfirmado());
        assertNull(vinculo.getChatPendiente());
    }

    @Test
    void elMismoChatPuedeReenviarElCodigoPeroOtroChatNoYLaConfirmacionLoConsume() {
        var vinculo = new VinculoTelegram(7L);
        vinculo.iniciar("hash", AHORA.plusSeconds(60));

        assertTrue(vinculo.aceptar("hash", 123L, AHORA));
        assertTrue(vinculo.aceptar("hash", 123L, AHORA), "un reintento de entrega no debe parecer un código vencido");
        assertFalse(vinculo.aceptar("hash", 456L, AHORA), "otro chat no puede usar un código ya aceptado");
        assertEquals(123L, vinculo.getChatPendiente());
        assertTrue(vinculo.confirmar(123L, AHORA));
        assertFalse(vinculo.aceptar("hash", 123L, AHORA), "una vez confirmado, el código ya no sirve");
    }

    @Test
    void rechazaElCodigoYLaConfirmacionAlVencer() {
        var vinculo = new VinculoTelegram(7L);
        var vencimiento = AHORA.plusSeconds(60);
        vinculo.iniciar("hash", vencimiento);

        assertFalse(vinculo.aceptar("hash", 123L, vencimiento));
        assertTrue(vinculo.aceptar("hash", 123L, AHORA));
        assertFalse(vinculo.confirmar(123L, vencimiento));
        assertNull(vinculo.getChatConfirmado());
    }
}
