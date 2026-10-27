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

    public StripeSdkGateway(@Value("${app.stripe.secret-key:}") String secretKey,
            @Value("${app.stripe.webhook-secret:}") String webhookSecret) {
        this.client = secretKey == null || secretKey.isBlank() ? null : new StripeClient(secretKey.trim());
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        if (client != null && this.webhookSecret.isEmpty()) {
            log.warn("STRIPE_WEBHOOK_SECRET is not set: subscriptions will not be updated from Stripe events");
        }
    }

    @Override
    public boolean isConfigured() {
        return client != null && !webhookSecret.isEmpty();
    }

    @Override
    public String crearCliente(String email, String nombre, Map<String, String> metadata) {
        return llamar(() -> requireClient().v1().customers().create(CustomerCreateParams.builder()
            .setEmail(email).setName(nombre).putAllMetadata(metadata).build()).getId());
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
            log.warn("Stripe event {} of type {} could not be deserialized; ignored", event.getId(), event.getType());
            return new EventoStripe(event.getId(), event.getType(), null, null);
        }
        if (objeto instanceof Subscription subscription) {
            return new EventoStripe(event.getId(), event.getType(), subscription.getId(), null);
        }
        if (objeto instanceof com.stripe.model.checkout.Session session) {
            return new EventoStripe(event.getId(), event.getType(), session.getSubscription(), null);
        }
        if (objeto instanceof Invoice invoice) {
            FacturaStripe factura = factura(invoice, event.getType());
            return new EventoStripe(event.getId(), event.getType(), factura.subscriptionId(), factura);
        }
        return new EventoStripe(event.getId(), event.getType(), null, null);
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
            finPeriodo, Boolean.TRUE.equals(s.getCancelAtPeriodEnd()));
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
        boolean pagada = "invoice.paid".equals(tipoEvento);
        Long monto = pagada ? invoice.getAmountPaid() : invoice.getAmountDue();
        Long pagadaEn = invoice.getStatusTransitions() == null ? null : invoice.getStatusTransitions().getPaidAt();
        return new FacturaStripe(invoice.getId(), subscriptionId, invoice.getCustomer(), monto == null ? 0L : monto,
            invoice.getCurrency(), pagada ? "PAGADO" : "FALLIDO",
            pagadaEn == null ? Instant.now() : Instant.ofEpochSecond(pagadaEn));
    }

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
