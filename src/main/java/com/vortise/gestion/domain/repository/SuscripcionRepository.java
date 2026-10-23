package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.Suscripcion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository {
    @EntityGraph(attributePaths = {"usuario", "usuario.empresa", "empresa", "plan"})
    @Query("select s from Suscripcion s left join s.usuario u left join u.empresa ue left join s.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId) and (:estado is null or lower(s.estado) = lower(:estado)) and (:empresaId is null or e.id = :empresaId or ue.id = :empresaId) order by s.actualizadoEn desc")
    Page<Suscripcion> buscarSuperAdmin(@Param("empresaInternaId") Long empresaInternaId, @Param("estado") String estado,
        @Param("empresaId") Long empresaId, Pageable pageable);

    @Query("select s.estado, count(s) from Suscripcion s left join s.usuario u left join s.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId) group by s.estado")
    List<Object[]> contarEstadosSuperAdmin(@Param("empresaInternaId") Long empresaInternaId);

    @Query("select count(s) from Suscripcion s left join s.usuario u left join s.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId) and s.estado = :estado")
    long contarEstadoSuperAdmin(@Param("empresaInternaId") Long empresaInternaId, @Param("estado") String estado);

    @Query("select count(s) from Suscripcion s left join s.usuario u left join s.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId) and s.estado is not null and s.estado not in ('active', 'trialing', 'past_due')")
    long contarInactivasSuperAdmin(@Param("empresaInternaId") Long empresaInternaId);

    @Query("select count(s) from Suscripcion s left join s.usuario u left join s.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId) and s.estado = 'trialing' and s.finPrueba between :desde and :hasta")
    long contarPruebasPorVencer(@Param("empresaInternaId") Long empresaInternaId, @Param("desde") java.time.Instant desde,
        @Param("hasta") java.time.Instant hasta);

    @Query("select max(s.actualizadoEn) from Suscripcion s left join s.usuario u left join s.empresa e where (u is null or u.superAdmin = false) and (:empresaInternaId is null or e.id is null or e.id <> :empresaInternaId)")
    java.time.Instant ultimaActualizacionSuperAdmin(@Param("empresaInternaId") Long empresaInternaId);

    @EntityGraph(attributePaths = "plan")
    java.util.Optional<Suscripcion> findFirstByEmpresaId(Long empresaId);

    Optional<Suscripcion> findByUsuarioId(Long usuarioId);
    Optional<Suscripcion> findByEmpresaId(Long empresaId);
    Optional<Suscripcion> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<Suscripcion> findByStripeCustomerId(String stripeCustomerId);
    List<Suscripcion> findByUsuario_Empresa_Id(Long empresaId);
    Suscripcion save(Suscripcion suscripcion);
}
