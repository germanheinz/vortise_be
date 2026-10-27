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

    FacturaStripe obtenerFactura(String invoiceId);

    DevolucionStripe obtenerDevolucion(String refundId);

    SuscripcionStripe obtenerSuscripcion(String subscriptionId);

    /** Finds the monthly price by lookup key or creates product and price; returns the price id. */
    String asegurarPrecioMensual(String lookupKey, String nombreProducto, long centavos, String moneda);

    record CheckoutStripe(String customerId, String priceId, Integer diasPrueba, String successUrl, String cancelUrl,
                          String clientReferenceId, Map<String, String> metadata) { }

    record SuscripcionStripe(String id, String customerId, String estado, String priceId, Instant finPrueba,
                             Instant finPeriodoActual, boolean cancelaAlFinal, Instant inicio) {
        public SuscripcionStripe(String id, String customerId, String estado, String priceId, Instant finPrueba,
                Instant finPeriodoActual, boolean cancelaAlFinal) {
            this(id, customerId, estado, priceId, finPrueba, finPeriodoActual, cancelaAlFinal, null);
        }
    }

    record FacturaStripe(String id, String subscriptionId, String customerId, long montoCentavos, String moneda,
                         String estado, Instant fecha, Long intentos, Instant proximoIntento, String estadoFactura,
                         Instant fechaPago, Instant periodoInicio, Instant periodoFin) {
        public FacturaStripe(String id, String subscriptionId, String customerId, long montoCentavos, String moneda,
                String estado, Instant fecha) {
            this(id, subscriptionId, customerId, montoCentavos, moneda, estado, fecha, null, null, null, null, null, null);
        }
    }

    record DevolucionStripe(String id, FacturaStripe factura, long montoCentavos, String estado, Instant fecha) { }

    record EventoStripe(String id, String tipo, String subscriptionId, FacturaStripe factura, String refundId) {
        public EventoStripe(String id, String tipo, String subscriptionId, FacturaStripe factura) {
            this(id, tipo, subscriptionId, factura, null);
        }
    }
}
