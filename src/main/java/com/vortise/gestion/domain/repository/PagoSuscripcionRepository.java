package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.PagoSuscripcion;
import java.util.Optional;

public interface PagoSuscripcionRepository {
    Optional<PagoSuscripcion> findByStripeInvoiceId(String stripeInvoiceId);
    Optional<PagoSuscripcion> findFirstBySuscripcionIdOrderByFechaDesc(Long suscripcionId);
    PagoSuscripcion save(PagoSuscripcion pago);
}
