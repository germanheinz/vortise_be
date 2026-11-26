package com.vortise.gestion.repository;

import com.vortise.gestion.model.InvitacionUsuario;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitacionUsuarioRepository {
    InvitacionUsuario save(InvitacionUsuario invitacion);
    Optional<InvitacionUsuario> findByTokenHash(String hash);
    Optional<InvitacionUsuario> findById(Long id);
    Optional<InvitacionUsuario> bloquearPorId(Long id);
    List<InvitacionUsuario> findByEmpresaIdOrderByIdDesc(Long empresaId);

    @Query("select count(i) from InvitacionUsuario i where i.empresaId = :empresaId and i.revocadoEn is null and i.consumidoEn is null and i.venceEn > :ahora")
    long contarPendientes(@Param("empresaId") Long empresaId, @Param("ahora") Instant ahora);

    @Query("select count(i) > 0 from InvitacionUsuario i where i.empresaId = :empresaId and i.email = :email and i.revocadoEn is null and i.consumidoEn is null and i.venceEn > :ahora")
    boolean existePendiente(@Param("empresaId") Long empresaId, @Param("email") String email, @Param("ahora") Instant ahora);
}
