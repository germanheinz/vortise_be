package com.vortise.gestion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import com.stripe.model.Invoice;
import com.vortise.gestion.application.port.StripeGateway.FacturaStripe;
import com.vortise.gestion.infrastructure.stripe.StripeSdkGateway;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InvoiceLifecycleMappingTest {
    private Invoice invoice(String status, boolean attempted) {
        var invoice = new Invoice();
        invoice.setId("in_test"); invoice.setCustomer("cus_test"); invoice.setCurrency("usd");
        invoice.setStatus(status); invoice.setAttempted(attempted);
        invoice.setAmountDue(5000L); invoice.setAmountPaid("paid".equals(status) ? 5000L : 0L);
        invoice.setCreated(1793110708L); invoice.setAttemptCount(attempted ? 1L : 0L);
        invoice.setPeriodStart(1793110708L); invoice.setPeriodEnd(1795702708L);
        return invoice;
    }

    private FacturaStripe map(Invoice invoice, String event) {
        return ReflectionTestUtils.invokeMethod(StripeSdkGateway.class, "factura", invoice, event);
    }

    @Test
    void currentPaidInvoiceWinsOverAnOlderFailureEvent() {
        var invoice = invoice("paid", true);
        invoice.setAttemptCount(2L); invoice.setNextPaymentAttempt(1795802708L);
        var transitions = new Invoice.StatusTransitions(); transitions.setPaidAt(1795702808L);
        invoice.setStatusTransitions(transitions);
        var result = map(invoice, "invoice.payment_failed");
        assertEquals("PAGADO", result.estado());
        assertEquals(2L, result.intentos());
        assertNull(result.proximoIntento());
        assertEquals(Instant.ofEpochSecond(1795702808L), result.fechaPago());
        assertEquals(result.fechaPago(), result.fecha());
    }

    @Test
    void pendingInvoiceIsNotAFailedChargeAndUsesStripeCreationDate() {
        var result = map(invoice("open", false), "invoice.updated");
        assertEquals("PENDIENTE", result.estado());
        assertEquals(0L, result.intentos());
        assertNull(result.fechaPago());
        assertEquals(Instant.ofEpochSecond(1793110708L), result.fecha());
    }

    @Test
    void currentPaymentIntentCanRequireCustomerAction() {
        var result = map(invoice("open", true), "invoice.payment_action_required");
        assertEquals("REQUIERE_ACCION", result.estado());
        assertNull(result.fechaPago());
    }

    @Test
    void failedInvoiceExposesRetryWithoutInventingAPaymentDate() {
        var invoice = invoice("open", true); invoice.setNextPaymentAttempt(1795802708L);
        var result = map(invoice, "invoice.payment_failed");
        assertEquals("FALLIDO", result.estado());
        assertEquals("open", result.estadoFactura());
        assertNull(result.fechaPago());
        assertEquals(Instant.ofEpochSecond(1795802708L), result.proximoIntento());
    }
}
