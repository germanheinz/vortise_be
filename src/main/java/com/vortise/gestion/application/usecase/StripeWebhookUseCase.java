package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.ConflictoEstadoException;
import com.vortise.gestion.application.exception.PagosNoConfiguradosException;
import com.vortise.gestion.application.port.StripeGateway.EventoStripe;
import com.vortise.gestion.application.port.StripeGateway.FacturaStripe;
import com.vortise.gestion.application.port.StripeGateway.SuscripcionStripe;
import com.vortise.gestion.application.port.StripeGateway;
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
        "invoice.paid", "invoice.payment_failed", "invoice.updated", "invoice.voided", "invoice.marked_uncollectible", "invoice.payment_action_required", "refund.created", "refund.updated", "refund.failed");
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
            if (evento.refundId() != null) {
                var ruta = stripe.obtenerDevolucion(evento.refundId());
                if (ruta.factura() != null && suscripciones.bloquearPorStripeCustomerId(ruta.factura().customerId()).isPresent()) {
                    // The first lookup is routing only. Never apply a snapshot read before the lock.
                    var refund = stripe.obtenerDevolucion(evento.refundId());
                    if (refund.factura() == null || !java.util.Objects.equals(ruta.factura().customerId(), refund.factura().customerId())) {
                        throw new ConflictoEstadoException("Cambió el titular de la devolución");
                    }
                    registrarPago(refund.factura());
                    pagos.findByStripeInvoiceId(refund.factura().id()).ifPresent(pago -> {
                        pago.registrarDevolucion(refund.id(), refund.montoCentavos(), refund.estado(), refund.fecha());
                        pagos.save(pago);
                    });
                }
            }
        }
        eventos.save(new StripeEventoProcesado(evento.id(), evento.tipo()));
    }

    /** Always re-reads the subscription from Stripe, so event delivery order does not matter. */
    private void sincronizar(String subscriptionId) {
        SuscripcionStripe ruta = stripe.obtenerSuscripcion(subscriptionId);
        Optional<Suscripcion> local = suscripciones.bloquearPorStripeCustomerId(ruta.customerId());
        if (local.isEmpty()) {
            log.warn("Stripe subscription {} does not belong to a known customer; ignored", subscriptionId);
            return;
        }
        Suscripcion suscripcion = local.get();
        SuscripcionStripe remota = stripe.obtenerSuscripcion(subscriptionId);
        if (!java.util.Objects.equals(suscripcion.getStripeCustomerId(), remota.customerId())) {
            throw new ConflictoEstadoException("Cambió el titular de la suscripción");
        }
        boolean otraSuscripcionActiva = !java.util.Objects.equals(subscriptionId, suscripcion.getStripeSubscriptionId()) && suscripcion.getStripeSubscriptionId() != null
            && suscripcion.isActiva(Instant.now()) && !ESTADOS_ACTIVOS.contains(remota.estado());
        if (otraSuscripcionActiva) return;
        suscripcion.actualizarDesdeStripe(remota.id(),
            remota.priceId() == null ? null : planes.findByStripePriceId(remota.priceId()).orElse(null),
            remota.estado(), remota.finPrueba(), remota.finPeriodoActual(), remota.cancelaAlFinal());
        suscripcion.registrarInicio(remota.inicio());
        suscripciones.save(suscripcion);
    }

    private void registrarPago(FacturaStripe ruta) {
        Optional<Suscripcion> suscripcion = suscripciones.bloquearPorStripeCustomerId(ruta.customerId());
        if (suscripcion.isEmpty()) {
            log.warn("Stripe invoice {} does not belong to a known customer; ignored", ruta.id());
            return;
        }
        FacturaStripe factura = stripe.obtenerFactura(ruta.id());
        if (!java.util.Objects.equals(suscripcion.get().getStripeCustomerId(), factura.customerId())) {
            throw new ConflictoEstadoException("Cambió el titular de la factura");
        }
        if ("draft".equals(factura.estadoFactura())) return;
        PagoSuscripcion pago = pagos.findByStripeInvoiceId(factura.id()).orElseGet(() -> new PagoSuscripcion(suscripcion.get(), factura.id()));
        pago.registrar(factura.montoCentavos(), factura.moneda(), factura.estado(), factura.fecha());
        pago.registrarDetalleFactura(factura.intentos(), factura.proximoIntento(), factura.estadoFactura(),
            factura.fechaPago(), factura.periodoInicio(), factura.periodoFin());
        pagos.save(pago);
    }
}
