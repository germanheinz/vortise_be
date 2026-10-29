package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository {
    java.util.Optional<PasswordResetToken> findById(Long id);
    PasswordResetToken save(PasswordResetToken token);

    @Query("select t.usuarioId from PasswordResetToken t where t.tokenHash = :hash")
    Optional<Long> buscarUsuarioPorHash(@Param("hash") String hash);
}
