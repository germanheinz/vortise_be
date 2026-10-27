package com.vortise.gestion.infrastructure.stripe;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceListParams;
import com.vortise.gestion.application.exception.PagosNoConfiguradosException;
import com.vortise.gestion.application.exception.ProveedorPagoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.application.port.StripeGateway;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Only class that talks to the Stripe SDK. Keys come from the environment and are never logged. */
@Component
public class StripeSdkGateway implements StripeGateway {
    private static final Logger log = LoggerFactory.getLogger(StripeSdkGateway.class);

    private final StripeClient client;
    private final String webhookSecret;
    private final String testClock;

    @org.springframework.beans.factory.annotation.Autowired
    public StripeSdkGateway(@Value("${app.stripe.secret-key:}") String secretKey,
            @Value("${app.stripe.webhook-secret:}") String webhookSecret,
            @Value("${app.stripe.test-clock:}") String testClock) {
        this.testClock = testClock == null ? "" : testClock.trim();
        if (!this.testClock.isEmpty() && (secretKey == null || !(secretKey.startsWith("sk_test_") || secretKey.startsWith("rk_test_")))) {
            throw new IllegalArgumentException("Los test clocks requieren una clave de sandbox");
        }
        this.client = secretKey == null || secretKey.isBlank() ? null : new StripeClient(secretKey.trim());
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        if (client != null && this.webhookSecret.isEmpty()) {
            log.warn("STRIPE_WEBHOOK_SECRET is not set: subscriptions will not be updated from Stripe events");
        }
    }

    public StripeSdkGateway(String secretKey, String webhookSecret) {
        this(secretKey, webhookSecret, "");
    }

    @Override
    public boolean isConfigured() {
        return client != null && !webhookSecret.isEmpty();
    }

    @Override
    public String crearCliente(String email, String nombre, Map<String, String> metadata) {
        var params = CustomerCreateParams.builder().setEmail(email).setName(nombre).putAllMetadata(metadata);
        if (!testClock.isEmpty()) params.setTestClock(testClock);
        return llamar(() -> requireClient().v1().customers().create(params.build()).getId());
    }

    @Override
    public String crearCheckout(CheckoutStripe checkout) {
        var subscriptionData = com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder()
            .putAllMetadata(checkout.metadata());
        if (checkout.diasPrueba() != null) subscriptionData.setTrialPeriodDays(checkout.diasPrueba().longValue());
        var params = com.stripe.param.checkout.SessionCreateParams.builder()
            .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(checkout.customerId())
            .setClientReferenceId(checkout.clientReferenceId())
            .setSuccessUrl(checkout.successUrl())
            .setCancelUrl(checkout.cancelUrl())
            .addLineItem(com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                .setPrice(checkout.priceId()).setQuantity(1L).build())
            .setSubscriptionData(subscriptionData.build())
            .putAllMetadata(checkout.metadata())
            .build();
        return llamar(() -> requireClient().v1().checkout().sessions().create(params).getUrl());
    }

    @Override
    public String crearPortal(String customerId, String returnUrl) {
        var params = com.stripe.param.billingportal.SessionCreateParams.builder()
            .setCustomer(customerId).setReturnUrl(returnUrl).build();
        return llamar(() -> requireClient().v1().billingPortal().sessions().create(params).getUrl());
    }

    @Override
    public EventoStripe parsearWebhook(String payload, String signatureHeader) {
        if (webhookSecret.isEmpty()) throw new PagosNoConfiguradosException();
        if (payload == null || signatureHeader == null || signatureHeader.isBlank()) {
            throw new SolicitudInvalidaException("Firma de webhook inválida");
        }
        com.stripe.model.Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException | RuntimeException ex) {
            throw new SolicitudInvalidaException("Firma de webhook inválida");
        }
        StripeObject objeto;
        try {
            var deserializer = event.getDataObjectDeserializer();
            objeto = deserializer.getObject().isPresent() ? deserializer.getObject().get() : deserializer.deserializeUnsafe();
        } catch (Exception ex) {
            throw new SolicitudInvalidaException("Evento Stripe incompatible; no se procesó");
        }
        if (objeto instanceof com.stripe.model.Refund refund) {
            return new EventoStripe(event.getId(), event.getType(), null, null, refund.getId());
        }
        if (objeto instanceof Subscription subscription) {
            return new EventoStripe(event.getId(), event.getType(), subscription.getId(), null);
        }
        if (objeto instanceof com.stripe.model.checkout.Session session) {
            return new EventoStripe(event.getId(), event.getType(), session.getSubscription(), null);
        }
        if (objeto instanceof Invoice invoice) {
            // Fetch with the SDK version: CLI snapshots can use the account's older default version.
            Invoice current = llamar(() -> requireClient().v1().invoices().retrieve(invoice.getId()));
            if ("draft".equals(current.getStatus())) return new EventoStripe(event.getId(), event.getType(), null, null);
            FacturaStripe factura = factura(current, event.getType());
            return new EventoStripe(event.getId(), event.getType(), factura.subscriptionId(), factura);
        }
        return new EventoStripe(event.getId(), event.getType(), null, null);
    }

    @Override
    public FacturaStripe obtenerFactura(String invoiceId) {
        Invoice current = llamar(() -> requireClient().v1().invoices().retrieve(invoiceId));
        String tipo = "invoice.updated";
        if ("open".equals(current.getStatus())) {
            var payments = llamar(() -> requireClient().v1().invoicePayments().list(
                com.stripe.param.InvoicePaymentListParams.builder().setInvoice(invoiceId).setLimit(100L).build()));
            for (var payment : payments.autoPagingIterable()) {
                if (!Boolean.TRUE.equals(payment.getIsDefault()) || payment.getPayment() == null
                        || payment.getPayment().getPaymentIntent() == null) continue;
                var intent = llamar(() -> requireClient().v1().paymentIntents().retrieve(payment.getPayment().getPaymentIntent()));
                if ("requires_action".equals(intent.getStatus())) tipo = "invoice.payment_action_required";
                break;
            }
        }
        return factura(current, tipo);
    }

    @Override
    public DevolucionStripe obtenerDevolucion(String refundId) {
        return llamar(() -> {
            var refund = requireClient().v1().refunds().retrieve(refundId);
            String intent = refund.getPaymentIntent();
            if (intent == null && refund.getCharge() != null) {
                intent = requireClient().v1().charges().retrieve(refund.getCharge()).getPaymentIntent();
            }
            if (intent == null) return new DevolucionStripe(refundId, null, refund.getAmount(), refund.getStatus(), Instant.ofEpochSecond(refund.getCreated()));
            var links = requireClient().v1().invoicePayments().list(com.stripe.param.InvoicePaymentListParams.builder()
                .setPayment(com.stripe.param.InvoicePaymentListParams.Payment.builder().setPaymentIntent(intent)
                    .setType(com.stripe.param.InvoicePaymentListParams.Payment.Type.PAYMENT_INTENT).build()).setLimit(2L).build()).getData();
            if (links.size() > 1) throw new IllegalStateException("Devolución con asociación de factura ambigua");
            FacturaStripe factura = null;
            if (!links.isEmpty()) {
                var invoice = requireClient().v1().invoices().retrieve(links.get(0).getInvoice());
                factura = factura(invoice, "paid".equals(invoice.getStatus()) ? "invoice.paid" : "invoice.payment_failed");
            }
            return new DevolucionStripe(refundId, factura, refund.getAmount(), refund.getStatus(), Instant.ofEpochSecond(refund.getCreated()));
        });
    }

    @Override
    public SuscripcionStripe obtenerSuscripcion(String subscriptionId) {
        Subscription s = llamar(() -> requireClient().v1().subscriptions().retrieve(subscriptionId));
        var items = s.getItems() == null ? java.util.List.<SubscriptionItem>of() : s.getItems().getData();
        Instant finPeriodo = items.stream().map(SubscriptionItem::getCurrentPeriodEnd).filter(Objects::nonNull)
            .max(Long::compare).map(Instant::ofEpochSecond).orElse(null);
        String priceId = items.stream().map(SubscriptionItem::getPrice).filter(Objects::nonNull).map(Price::getId).findFirst().orElse(null);
        return new SuscripcionStripe(s.getId(), s.getCustomer(), s.getStatus(), priceId,
            s.getTrialEnd() == null ? null : Instant.ofEpochSecond(s.getTrialEnd()),
            finPeriodo, Boolean.TRUE.equals(s.getCancelAtPeriodEnd()),
            s.getStartDate() == null ? null : Instant.ofEpochSecond(s.getStartDate()));
    }

    @Override
    public String asegurarPrecioMensual(String lookupKey, String nombreProducto, long centavos, String moneda) {
        return llamar(() -> {
            var existentes = requireClient().v1().prices().list(PriceListParams.builder().addLookupKey(lookupKey).setActive(true).build()).getData();
            if (!existentes.isEmpty()) return existentes.get(0).getId();
            return requireClient().v1().prices().create(PriceCreateParams.builder()
                .setCurrency(moneda).setUnitAmount(centavos).setLookupKey(lookupKey)
                .setRecurring(PriceCreateParams.Recurring.builder().setInterval(PriceCreateParams.Recurring.Interval.MONTH).build())
                .setProductData(PriceCreateParams.ProductData.builder().setName(nombreProducto).build())
                .build()).getId();
        });
    }

    private static FacturaStripe factura(Invoice invoice, String tipoEvento) {
        String subscriptionId = invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null
            ? null : invoice.getParent().getSubscriptionDetails().getSubscription();
        boolean pagada = "paid".equals(invoice.getStatus());
        Long monto = pagada ? invoice.getAmountPaid() : invoice.getAmountDue();
        Long pagadaEn = invoice.getStatusTransitions() == null ? null : invoice.getStatusTransitions().getPaidAt();
        String estado = switch (invoice.getStatus() == null ? "" : invoice.getStatus()) {
            case "paid" -> "PAGADO";
            case "void" -> "ANULADO";
            case "uncollectible" -> "INCOBRABLE";
            default -> "invoice.payment_action_required".equals(tipoEvento) ? "REQUIERE_ACCION"
                : Boolean.TRUE.equals(invoice.getAttempted()) ? "FALLIDO" : "PENDIENTE";
        };
        Instant fechaPago = instant(pagadaEn);
        return new FacturaStripe(invoice.getId(), subscriptionId, invoice.getCustomer(), monto == null ? 0L : monto,
            invoice.getCurrency(), estado,
            fechaPago == null ? Instant.ofEpochSecond(Objects.requireNonNull(invoice.getCreated(), "Factura sin fecha de creación")) : fechaPago,
            invoice.getAttemptCount(), pagada ? null : instant(invoice.getNextPaymentAttempt()), invoice.getStatus(),
            fechaPago, instant(invoice.getPeriodStart()), instant(invoice.getPeriodEnd()));
    }

    private static Instant instant(Long seconds) { return seconds == null ? null : Instant.ofEpochSecond(seconds); }

    private StripeClient requireClient() {
        if (client == null) throw new PagosNoConfiguradosException();
        return client;
    }

    private <T> T llamar(StripeCall<T> call) {
        try {
            return call.run();
        } catch (StripeException ex) {
            log.error("Stripe request failed: {} (request id {})", ex.getCode(), ex.getRequestId());
            throw new ProveedorPagoException(ex);
        }
    }

    @FunctionalInterface
    private interface StripeCall<T> {
        T run() throws StripeException;
    }
}
