package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.StripeEventoProcesado;

public interface StripeEventoProcesadoRepository {
    boolean existsById(String eventId);
    StripeEventoProcesado save(StripeEventoProcesado evento);
}
