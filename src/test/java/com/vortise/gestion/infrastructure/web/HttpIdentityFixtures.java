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
        if (role == RolEmpresa.COLABORADOR) {
            var owner = user(em, company, RolEmpresa.ADMINISTRADOR, false);
            em.getReference(Empresa.class, company).setCreadoPorUsuarioId(owner.getId());
            user.asignarEquipo(owner.getId(), java.util.Set.of(company));
            var plans = em.createQuery("select p from PlanSuscripcion p where p.codigo = 'PRO'", PlanSuscripcion.class).getResultList();
            PlanSuscripcion pro;
            if (plans.isEmpty()) { pro = new PlanSuscripcion("PRO", "Pro fixture", 1, "usd", 0, 2, java.util.List.of()); em.persist(pro); }
            else pro = plans.get(0);
            var subscription = Suscripcion.paraUsuario(owner, "cus_http_fixture_"+java.util.UUID.randomUUID());
            subscription.actualizarDesdeStripe("sub_http_fixture_"+java.util.UUID.randomUUID(), pro, "active", null, java.time.Instant.now().plusSeconds(3600), false);
            em.persist(subscription);
        }
        user.setSuperAdmin(superAdmin); em.persist(user); em.flush(); return user;
    }
    static String token(EntityManager em, JwtService jwt, Long company, RolEmpresa role, boolean admin) {
        var u = user(em, company, role, admin);
        return jwt.generate(u.getId(), company, u.getEmail(), u.getRol().name(), u.isSuperAdmin(), u.getSessionVersion());
    }
}
