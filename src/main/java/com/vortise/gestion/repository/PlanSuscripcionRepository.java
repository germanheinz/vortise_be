package com.vortise.gestion.repository;

import com.vortise.gestion.model.PlanSuscripcion;
import java.util.List;
import java.util.Optional;

public interface PlanSuscripcionRepository {
    Optional<PlanSuscripcion> findByCodigo(String codigo);
    Optional<PlanSuscripcion> findByStripePriceId(String stripePriceId);
    List<PlanSuscripcion> findByActivoTrueOrderByNivelAsc();
    PlanSuscripcion save(PlanSuscripcion plan);
}
