package com.vortise.gestion.repository;

import com.vortise.gestion.model.StripeEventoProcesado;

public interface StripeEventoProcesadoRepository {
    boolean existsById(String eventId);
    StripeEventoProcesado save(StripeEventoProcesado evento);
}
