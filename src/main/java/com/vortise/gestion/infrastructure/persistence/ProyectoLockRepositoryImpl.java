package com.vortise.gestion.infrastructure.persistence;

import com.vortise.gestion.domain.model.Proyecto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;

/** Reload a cached aggregate under its row lock without cascading REFRESH over every collection. */
public class ProyectoLockRepositoryImpl implements ProyectoLockRepository {
    private final EntityManager entityManager;
    public ProyectoLockRepositoryImpl(EntityManager entityManager) { this.entityManager = entityManager; }
    public Optional<Proyecto> bloquearPorId(Long id) {
        Proyecto value = entityManager.find(Proyecto.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (value != null) {
            // Preserve pending work, evict only this aggregate, and reload while the DB lock remains held.
            // Unlike refresh with cascade=ALL, detach does not initialize lazy collections.
            entityManager.flush();
            entityManager.detach(value);
            value = entityManager.find(Proyecto.class, id, LockModeType.PESSIMISTIC_WRITE);
        }
        return Optional.ofNullable(value);
    }
}
