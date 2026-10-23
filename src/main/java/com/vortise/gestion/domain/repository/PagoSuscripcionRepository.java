package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.PagoSuscripcion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface PagoSuscripcionRepository {
    @EntityGraph(attributePaths = {"suscripcion", "suscripcion.usuario", "suscripcion.usuario.empresa", "suscripcion.empresa", "suscripcion.plan"})
    @Query("select p from PagoSuscripcion p left join p.suscripcion.usuario u left join p.suscripcion.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId) and (:estado is null or lower(p.estado) = lower(:estado)) and (:desde is null or p.fecha >= :desde) and (:hasta is null or p.fecha < :hasta) order by p.fecha desc")
    Page<PagoSuscripcion> buscarSuperAdmin(@Param("empresaInternaId") Long empresaInternaId, @Param("estado") String estado,
            @Param("desde") Instant desde, @Param("hasta") Instant hasta, Pageable pageable);

    @Query("select p.estado, p.moneda, count(p), sum(p.montoCentavos) from PagoSuscripcion p left join p.suscripcion.usuario u left join p.suscripcion.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId) and p.fecha >= :desde group by p.estado, p.moneda")
    java.util.List<Object[]> resumirRecientesSuperAdmin(@Param("empresaInternaId") Long empresaInternaId, @Param("desde") Instant desde);

    @Query("select count(p) from PagoSuscripcion p left join p.suscripcion.usuario u left join p.suscripcion.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId) and upper(p.estado) = 'FALLIDO'")
    long contarFallidosSuperAdmin(@Param("empresaInternaId") Long empresaInternaId);

    @Query("select max(p.fecha) from PagoSuscripcion p left join p.suscripcion.usuario u left join p.suscripcion.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId)")
    Instant ultimaActualizacionSuperAdmin(@Param("empresaInternaId") Long empresaInternaId);

    Optional<PagoSuscripcion> findByStripeInvoiceId(String stripeInvoiceId);
    Optional<PagoSuscripcion> findFirstBySuscripcionIdOrderByFechaDesc(Long suscripcionId);
    PagoSuscripcion save(PagoSuscripcion pago);
}
