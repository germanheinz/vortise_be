package com.vortise.gestion.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.StripeGateway;
import com.vortise.gestion.application.port.StripeGateway.EventoStripe;
import com.vortise.gestion.application.port.StripeGateway.FacturaStripe;
import com.vortise.gestion.application.port.StripeGateway.SuscripcionStripe;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.PagoSuscripcion;
import com.vortise.gestion.domain.model.PlanSuscripcion;
import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.Suscripcion;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.PagoSuscripcionRepository;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import com.vortise.gestion.domain.repository.StripeEventoProcesadoRepository;
import com.vortise.gestion.domain.repository.SuscripcionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeWebhookUseCaseTest {
    private final StripeGateway stripe = mock(StripeGateway.class);
    private final StripeEventoProcesadoRepository eventos = mock(StripeEventoProcesadoRepository.class);
    private final SuscripcionRepository suscripciones = mock(SuscripcionRepository.class);
    private final PlanSuscripcionRepository planes = mock(PlanSuscripcionRepository.class);
    private final PagoSuscripcionRepository pagos = mock(PagoSuscripcionRepository.class);
    private final StripeWebhookUseCase useCase = new StripeWebhookUseCase(stripe, eventos, suscripciones, planes, pagos);

    private final Instant fin = Instant.now().plus(30, ChronoUnit.DAYS);
    private final PlanSuscripcion pro = new PlanSuscripcion("PRO", "Pro", 200, "usd", 0, 2, List.of("x"));
    private Suscripcion local;

    @BeforeEach
    void setUp() {
        when(stripe.isConfigured()).thenReturn(true);
        local = Suscripcion.paraUsuario(new UsuarioEmpresa(new Empresa("E", null), "u@example.invalid", "U", "h", RolEmpresa.CONSULTA), "cus_1");
        when(suscripciones.findByStripeSubscriptionId(any())).thenReturn(Optional.empty());
        when(suscripciones.findByStripeCustomerId("cus_1")).thenReturn(Optional.of(local));
        when(planes.findByStripePriceId("price_pro")).thenReturn(Optional.of(pro));
        when(pagos.findByStripeInvoiceId(any())).thenReturn(Optional.empty());
    }

    @Test
    void invalidSignatureChangesNothing() {
        when(stripe.parsearWebhook("{}", "bad")).thenThrow(new SolicitudInvalidaException("Firma de webhook inválida"));
        assertThrows(SolicitudInvalidaException.class, () -> useCase.procesar("{}", "bad"));
        verifyNoInteractions(eventos, suscripciones, pagos);
    }

    @Test
    void duplicateEventIsIgnored() {
        when(stripe.parsearWebhook("p", "s")).thenReturn(new EventoStripe("evt_1", "customer.subscription.updated", "sub_1", null));
        when(eventos.existsById("evt_1")).thenReturn(true);
        useCase.procesar("p", "s");
        verify(stripe, never()).obtenerSuscripcion(any());
        verify(eventos, never()).save(any());
    }

    @Test
    void subscriptionEventSyncsStateFromStripeAndRecordsEvent() {
        when(stripe.parsearWebhook("p", "s")).thenReturn(new EventoStripe("evt_1", "checkout.session.completed", "sub_1", null));
        when(stripe.obtenerSuscripcion("sub_1")).thenReturn(new SuscripcionStripe("sub_1", "cus_1", "active", "price_pro", null, fin, false));
        useCase.procesar("p", "s");
        assertEquals("sub_1", local.getStripeSubscriptionId());
        assertEquals("active", local.getEstado());
        assertEquals(pro, local.getPlan());
        assertEquals(fin, local.getFinPeriodoActual());
        verify(suscripciones).save(local);
        verify(eventos).save(org.mockito.ArgumentMatchers.argThat(e -> "evt_1".equals(e.getEventId())));
    }

    @Test
    void staleEventFromOldSubscriptionDoesNotDowngradeNewActiveOne() {
        local.actualizarDesdeStripe("sub_new", pro, "active", null, fin, false);
        when(stripe.parsearWebhook("p", "s")).thenReturn(new EventoStripe("evt_2", "customer.subscription.deleted", "sub_old", null));
        when(stripe.obtenerSuscripcion("sub_old")).thenReturn(new SuscripcionStripe("sub_old", "cus_1", "canceled", "price_pro", null, null, false));
        useCase.procesar("p", "s");
        assertEquals("sub_new", local.getStripeSubscriptionId());
        assertEquals("active", local.getEstado());
        verify(suscripciones, never()).save(any());
    }

    @Test
    void unknownCustomerIsIgnored() {
        when(stripe.parsearWebhook("p", "s")).thenReturn(new EventoStripe("evt_3", "customer.subscription.created", "sub_x", null));
        when(stripe.obtenerSuscripcion("sub_x")).thenReturn(new SuscripcionStripe("sub_x", "cus_other", "active", "price_pro", null, fin, false));
        when(suscripciones.findByStripeCustomerId("cus_other")).thenReturn(Optional.empty());
        useCase.procesar("p", "s");
        verify(suscripciones, never()).save(any());
    }

    @Test
    void paidInvoiceIsRecordedOnce() {
        Instant pagado = Instant.parse("2026-10-01T10:00:00Z");
        FacturaStripe factura = new FacturaStripe("in_1", null, "cus_1", 200, "usd", "PAGADO", pagado);
        when(stripe.parsearWebhook("p", "s")).thenReturn(new EventoStripe("evt_4", "invoice.paid", null, factura));
        useCase.procesar("p", "s");
        verify(pagos).save(org.mockito.ArgumentMatchers.argThat((PagoSuscripcion pago) -> "in_1".equals(pago.getStripeInvoiceId())
            && pago.getMontoCentavos() == 200 && "PAGADO".equals(pago.getEstado()) && pagado.equals(pago.getFecha())
            && pago.getSuscripcion() == local));
    }

    @Test
    void unhandledEventTypeIsOnlyRecorded() {
        when(stripe.parsearWebhook("p", "s")).thenReturn(new EventoStripe("evt_5", "customer.created", null, null));
        useCase.procesar("p", "s");
        verify(stripe, never()).obtenerSuscripcion(any());
        verify(eventos).save(any());
    }
}
