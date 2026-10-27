package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import com.vortise.gestion.domain.model.PagoSuscripcion;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentRefundTest {
    @Test
    void confirmedRefundsPreserveGrossAndAreIdempotentAcrossUpdates() {
        var pago = new PagoSuscripcion(null, "in_test");
        Instant date = Instant.parse("2026-10-27T00:00:00Z");
        pago.registrar(9900, "usd", "PAGADO", date);
        pago.registrarDevolucion("re_1", 3000, "pending", date);
        assertEquals(0, pago.getMontoDevueltoCentavos());
        assertEquals("PENDIENTE", pago.getEstadoDevolucion());
        pago.registrarDevolucion("re_1", 3000, "succeeded", date);
        pago.registrarDevolucion("re_1", 3000, "succeeded", date);
        assertEquals(9900, pago.getMontoCentavos());
        assertEquals(3000, pago.getMontoDevueltoCentavos());
        assertEquals(6900, pago.getMontoNetoCentavos());
        assertEquals("PARCIAL", pago.getEstadoDevolucion());
        pago.registrarDevolucion("re_failed", 500, "failed", date);
        assertEquals(3000, pago.getMontoDevueltoCentavos());
        pago.registrarDevolucion("re_2", 6900, "succeeded", date);
        assertEquals("TOTAL", pago.getEstadoDevolucion());
        assertEquals(0, pago.getMontoNetoCentavos());
        // Later invoice event must not erase the refund history.
        pago.registrar(9900, "usd", "PAGADO", date);
        assertEquals(9900, pago.getMontoDevueltoCentavos());
    }

    @Test
    void failedRefundDoesNotReduceNetAndFailedPaymentHasNoConfirmedNet() {
        var pago = new PagoSuscripcion(null, "in_test");
        pago.registrar(5000, "usd", "PAGADO", Instant.now());
        pago.registrarDevolucion("re_failed", 5000, "failed", Instant.now());
        assertEquals("FALLIDA", pago.getEstadoDevolucion());
        assertEquals(5000, pago.getMontoNetoCentavos());
        pago.registrar(5000, "usd", "FALLIDO", Instant.now());
        assertEquals(0, pago.getMontoNetoCentavos());
    }
}
