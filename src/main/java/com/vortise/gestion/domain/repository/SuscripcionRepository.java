package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Suscripcion;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository {
    Optional<Suscripcion> findByUsuarioId(Long usuarioId);
    Optional<Suscripcion> findByEmpresaId(Long empresaId);
    Optional<Suscripcion> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<Suscripcion> findByStripeCustomerId(String stripeCustomerId);
    List<Suscripcion> findByUsuario_Empresa_Id(Long empresaId);
    Suscripcion save(Suscripcion suscripcion);
}
