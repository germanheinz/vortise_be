package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.PagosNoConfiguradosException;
import com.vortise.gestion.application.port.StripeGateway;
import com.vortise.gestion.application.port.StripeGateway.EventoStripe;
import com.vortise.gestion.application.port.StripeGateway.FacturaStripe;
import com.vortise.gestion.application.port.StripeGateway.SuscripcionStripe;
import com.vortise.gestion.domain.model.PagoSuscripcion;
import com.vortise.gestion.domain.model.StripeEventoProcesado;
import com.vortise.gestion.domain.model.Suscripcion;
import com.vortise.gestion.domain.repository.PagoSuscripcionRepository;
import com.vortise.gestion.domain.repository.PlanSuscripcionRepository;
import com.vortise.gestion.domain.repository.StripeEventoProcesadoRepository;
import com.vortise.gestion.domain.repository.SuscripcionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookUseCase {
    static final Set<String> EVENTOS_MANEJADOS = Set.of(
        "checkout.session.completed",
        "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted",
        "invoice.paid", "invoice.payment_failed");
    private static final Set<String> ESTADOS_ACTIVOS = Set.of("trialing", "active");
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookUseCase.class);

    private final StripeGateway stripe;
    private final StripeEventoProcesadoRepository eventos;
    private final SuscripcionRepository suscripciones;
    private final PlanSuscripcionRepository planes;
    private final PagoSuscripcionRepository pagos;

    public StripeWebhookUseCase(StripeGateway stripe, StripeEventoProcesadoRepository eventos, SuscripcionRepository suscripciones,
            PlanSuscripcionRepository planes, PagoSuscripcionRepository pagos) {
        this.stripe = stripe; this.eventos = eventos; this.suscripciones = suscripciones; this.planes = planes; this.pagos = pagos;
    }

    @Transactional
    public void procesar(String payload, String signatureHeader) {
        if (!stripe.isConfigured()) throw new PagosNoConfiguradosException();
        EventoStripe evento = stripe.parsearWebhook(payload, signatureHeader);
        if (eventos.existsById(evento.id())) return;
        if (EVENTOS_MANEJADOS.contains(evento.tipo())) {
            if (evento.subscriptionId() != null) sincronizar(evento.subscriptionId());
            if (evento.factura() != null) registrarPago(evento.factura());
        }
        eventos.save(new StripeEventoProcesado(evento.id(), evento.tipo()));
    }

    /** Always re-reads the subscription from Stripe, so event delivery order does not matter. */
    private void sincronizar(String subscriptionId) {
        SuscripcionStripe remota = stripe.obtenerSuscripcion(subscriptionId);
        Optional<Suscripcion> porId = suscripciones.findByStripeSubscriptionId(subscriptionId);
        Optional<Suscripcion> local = porId.or(() -> suscripciones.findByStripeCustomerId(remota.customerId()));
        if (local.isEmpty()) {
            log.warn("Stripe subscription {} does not belong to a known customer; ignored", subscriptionId);
            return;
        }
        Suscripcion suscripcion = local.get();
        boolean otraSuscripcionActiva = porId.isEmpty() && suscripcion.getStripeSubscriptionId() != null
            && suscripcion.isActiva(Instant.now()) && !ESTADOS_ACTIVOS.contains(remota.estado());
        if (otraSuscripcionActiva) return;
        suscripcion.actualizarDesdeStripe(remota.id(),
            remota.priceId() == null ? null : planes.findByStripePriceId(remota.priceId()).orElse(null),
            remota.estado(), remota.finPrueba(), remota.finPeriodoActual(), remota.cancelaAlFinal());
        suscripciones.save(suscripcion);
    }

    private void registrarPago(FacturaStripe factura) {
        Optional<Suscripcion> suscripcion = Optional.ofNullable(factura.subscriptionId())
            .flatMap(suscripciones::findByStripeSubscriptionId)
            .or(() -> Optional.ofNullable(factura.customerId()).flatMap(suscripciones::findByStripeCustomerId));
        if (suscripcion.isEmpty()) {
            log.warn("Stripe invoice {} does not belong to a known customer; ignored", factura.id());
            return;
        }
        PagoSuscripcion pago = pagos.findByStripeInvoiceId(factura.id()).orElseGet(() -> new PagoSuscripcion(suscripcion.get(), factura.id()));
        pago.registrar(factura.montoCentavos(), factura.moneda(), factura.estado(), factura.fecha());
        pagos.save(pago);
    }
}
