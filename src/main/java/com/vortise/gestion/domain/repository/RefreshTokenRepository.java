package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);

    // The row lock serializes concurrent rotations of the same token.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> bloquearPorHash(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshToken t set t.revocadoEn = :ahora where t.familia = :familia and t.revocadoEn is null")
    int revocarFamilia(@Param("familia") String familia, @Param("ahora") Instant ahora);

    @Modifying
    @Query("update RefreshToken t set t.revocadoEn = :ahora where t.usuarioId = :usuarioId and t.revocadoEn is null")
    int revocarPorUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

    @Modifying
    @Query("delete from RefreshToken t where t.venceEn < :antes")
    int limpiarVencidos(@Param("antes") Instant antes);
}
