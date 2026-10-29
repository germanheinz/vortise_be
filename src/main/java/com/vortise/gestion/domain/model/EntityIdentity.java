package com.vortise.gestion.domain.model;

import org.hibernate.proxy.HibernateProxy;

/** Stable identity for the existing JPA model; never traverse mutable relationships. */
final class EntityIdentity {
    private EntityIdentity() { }
    static Class<?> type(Object entity) {
        return entity instanceof HibernateProxy proxy
            ? proxy.getHibernateLazyInitializer().getPersistentClass() : entity.getClass();
    }
}
