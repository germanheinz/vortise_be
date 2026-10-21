package com.vortise.gestion.application.port;

import java.time.Instant;
import java.util.Map;

public interface StripeGateway {
    boolean isConfigured();

    String crearCliente(String email, String nombre, Map<String, String> metadata);

    /** Returns the hosted Checkout URL. */
    String crearCheckout(CheckoutStripe checkout);

    /** Returns the hosted Customer Portal URL. */
    String crearPortal(String customerId, String returnUrl);

    /** Verifies the signature; throws SolicitudInvalidaException when it is invalid. */
    EventoStripe parsearWebhook(String payload, String signatureHeader);

    SuscripcionStripe obtenerSuscripcion(String subscriptionId);

    /** Finds the monthly price by lookup key or creates product and price; returns the price id. */
    String asegurarPrecioMensual(String lookupKey, String nombreProducto, long centavos, String moneda);

    record CheckoutStripe(String customerId, String priceId, Integer diasPrueba, String successUrl, String cancelUrl,
                          String clientReferenceId, Map<String, String> metadata) { }

    record SuscripcionStripe(String id, String customerId, String estado, String priceId, Instant finPrueba,
                             Instant finPeriodoActual, boolean cancelaAlFinal) { }

    record FacturaStripe(String id, String subscriptionId, String customerId, long montoCentavos, String moneda,
                         String estado, Instant fecha) { }

    record EventoStripe(String id, String tipo, String subscriptionId, FacturaStripe factura) { }
}
