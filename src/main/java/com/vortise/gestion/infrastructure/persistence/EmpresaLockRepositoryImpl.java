package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Empresa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;

/** Refresh also covers entities loaded by the HTTP tenant interceptor before the lock. */
public class EmpresaLockRepositoryImpl implements EmpresaLockRepository {
    private final EntityManager entityManager;
    public EmpresaLockRepositoryImpl(EntityManager entityManager) { this.entityManager = entityManager; }
    public Optional<Empresa> bloquearPorId(Long id) {
        Empresa value = entityManager.find(Empresa.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (value != null) {
            // Preserve pending changes from company creation before refreshing its cached instance.
            // Callers acquire this lock before mutating the existing aggregate.
            entityManager.flush();
            entityManager.refresh(value, LockModeType.PESSIMISTIC_WRITE);
        }
        return Optional.ofNullable(value);
    }
}
