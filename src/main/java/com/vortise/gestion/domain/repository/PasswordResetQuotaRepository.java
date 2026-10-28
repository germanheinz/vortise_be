package com.vortise.gestion.domain.repository;

import com.vortise.gestion.domain.model.PasswordResetQuota;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PasswordResetQuotaRepository {
    java.util.Optional<PasswordResetQuota> findById(String key);
    PasswordResetQuota save(PasswordResetQuota quota);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from PasswordResetQuota q where q.clave = 'global'")
    Optional<PasswordResetQuota> bloquearGuard();
    @Modifying
    @Query("delete from PasswordResetQuota q where q.clave <> 'global' and q.inicio < :antes")
    void limpiar(@Param("antes") Instant antes);
}
