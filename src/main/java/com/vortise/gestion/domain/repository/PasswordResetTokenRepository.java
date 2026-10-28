package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepository {
    java.util.Optional<PasswordResetToken> findById(Long id);
    PasswordResetToken save(PasswordResetToken token);

    @org.springframework.data.jpa.repository.Query("select t.usuarioId from PasswordResetToken t where t.tokenHash = :hash")
    Optional<Long> buscarUsuarioPorHash(@org.springframework.data.repository.query.Param("hash") String hash);
}
