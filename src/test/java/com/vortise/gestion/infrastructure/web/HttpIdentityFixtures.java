package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.*;
import jakarta.persistence.EntityManager;

/** Must be called in the test's reviewed ephemeral transaction. Real persisted identities. */
final class HttpIdentityFixtures {
    static UsuarioEmpresa user(EntityManager em, Long company, RolEmpresa role, boolean superAdmin) {
        String email = "http-" + company + "-" + role.name() + "-" + superAdmin + "@example.invalid";
        var found = em.createQuery("select u from UsuarioEmpresa u where u.email = :email", UsuarioEmpresa.class)
            .setParameter("email", email).getResultStream().findFirst();
        if (found.isPresent()) return found.get();
        var user = new UsuarioEmpresa(em.getReference(Empresa.class, company), email, "HTTP fixture", "not-a-login-password", role);
        user.setSuperAdmin(superAdmin); em.persist(user); em.flush(); return user;
    }
    static String token(EntityManager em, JwtService jwt, Long company, RolEmpresa role, boolean admin) {
        var u = user(em, company, role, admin);
        return jwt.generate(u.getId(), company, u.getEmail(), u.getRol().name(), u.isSuperAdmin(), u.getSessionVersion());
    }
}
