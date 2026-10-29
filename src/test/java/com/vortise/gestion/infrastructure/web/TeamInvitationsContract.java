package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.infrastructure.config.*;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class TeamInvitationsContract {
    @MockitoBean DataSeeder dataSeeder;
    @MockitoBean UsuarioDemoSeeder demoSeeder;
    @MockitoBean PlanSuscripcionSeeder planSeeder;
    @MockitoBean SuperAdminSeeder adminSeeder;
    @MockitoBean com.vortise.gestion.application.port.FotoPerfilStorage photos;
    @Autowired DataSource source;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    protected Long ownerId, firstId, secondId, excludedId, subscriptionId, invitedId;
    protected String admin;
    protected static final String PASSWORD = "Synthetic-invitation-only-26!";
    @Autowired com.vortise.gestion.application.usecase.InvitacionesUseCase invitations;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired tools.jackson.databind.ObjectMapper json;

    protected abstract void requireOwned() throws Exception;
    @BeforeEach void prepare() throws Exception {
        requireOwned();
        new TransactionTemplate(tm).executeWithoutResult(tx -> {
            Empresa a = new Empresa("Team A " + UUID.randomUUID(), null), b = new Empresa("Team B " + UUID.randomUUID(), null), x = new Empresa("Team excluded " + UUID.randomUUID(), null);
            em.persist(a); em.persist(b); em.persist(x); em.flush();
            var owner = new UsuarioEmpresa(a, UUID.randomUUID()+"@example.invalid", "Titular", encoder.encode(PASSWORD), RolEmpresa.ADMINISTRADOR);
            em.persist(owner); em.flush(); ownerId = owner.getId();
            a.setCreadoPorUsuarioId(ownerId); b.setCreadoPorUsuarioId(ownerId); x.setCreadoPorUsuarioId(ownerId);
            firstId = a.getId(); secondId = b.getId(); excludedId = x.getId();
            var pro = new PlanSuscripcion("PRO-"+UUID.randomUUID(), "Pro fixture", 1, "usd", 0, 2, List.of());
            // Use one canonical PRO plan; independent test transactions can reuse the fixture catalog.
            var plans = em.createQuery("select p from PlanSuscripcion p where p.codigo = 'PRO'", PlanSuscripcion.class).getResultList();
            if (plans.isEmpty()) { pro = new PlanSuscripcion("PRO", "Pro", 1, "usd", 0, 2, List.of()); em.persist(pro); } else pro = plans.get(0);
            var subscription = Suscripcion.paraUsuario(owner, "cus_fixture_"+UUID.randomUUID());
            subscription.actualizarDesdeStripe("sub_fixture_"+UUID.randomUUID(), pro, "active", null, java.time.Instant.now().plusSeconds(86400), false);
            em.persist(subscription); em.flush(); subscriptionId = subscription.getId();
            admin = "Bearer " + jwt.generate(ownerId, firstId, owner.getEmail(), "ADMINISTRADOR", false, owner.getSessionVersion());
        });
    }
    protected String createLink() throws Exception {
        var response = mvc.perform(post("/api/equipo/invitaciones").header("Authorization", admin).contentType("application/json")
            .content("{\"empresaIds\":["+firstId+","+secondId+"]}")).andExpect(status().isCreated()).andReturn();
        String link = json.readTree(response.getResponse().getContentAsString()).get("enlace").asText();
        return link.substring(link.indexOf("#token=")+7);
    }
    protected String accept(String token) throws Exception {
        var dto = new com.vortise.gestion.application.dto.InvitacionesDto.Aceptar(token, "Colaborador", UUID.randomUUID()+"@example.invalid", PASSWORD);
        var result = mvc.perform(post("/api/auth/invitaciones/aceptar").contentType("application/json").content(json.writeValueAsString(dto)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("COLABORADOR")).andExpect(jsonPath("$.planCodigo").value("PRO")).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString()); invitedId = body.get("usuarioId").asLong();
        return "Bearer " + body.get("token").asText();
    }
    @Test void multiCompanyInvitationGrantsOnlyAssignedAccessAndCannotBeReused() throws Exception {
        String token = createLink(), collaborator = accept(token);
        mvc.perform(get("/api/empresas").header("Authorization", collaborator)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        for (Long id : List.of(firstId, secondId)) {
            mvc.perform(get("/api/empresas/"+id+"/obras/pagina").header("Authorization", collaborator)).andExpect(status().isOk());
            mvc.perform(get("/api/empresas/"+id+"/configuracion/unidades").header("Authorization", collaborator)).andExpect(status().isOk());
            mvc.perform(get("/api/empresas/"+id+"/usuarios").header("Authorization", collaborator)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/empresas/"+excludedId).header("Authorization", collaborator)).andExpect(status().isForbidden());
        mvc.perform(post("/api/empresas").header("Authorization", collaborator).contentType("application/json").content("{\"nombre\":\"Denied\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/proyectos").header("Authorization", collaborator).contentType("application/json").content("{\"nombre\":\"Denied\",\"direccion\":\"Synthetic\",\"numeroProyecto\":\"N1\",\"empresaId\":"+firstId+"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/invitaciones/aceptar").contentType("application/json").content(json.writeValueAsString(
            new com.vortise.gestion.application.dto.InvitacionesDto.Aceptar(token, "Replay", UUID.randomUUID()+"@example.invalid", PASSWORD))))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/equipo/invitaciones").header("Authorization", collaborator).contentType("application/json").content("{\"empresaIds\":["+firstId+"]}")).andExpect(status().isForbidden());
    }
    @Test void losingProSuspendsExistingBearerAndRestoringProRestoresAccess() throws Exception {
        String collaborator = accept(createLink());
        new TransactionTemplate(tm).executeWithoutResult(tx -> {
            var sub = em.find(Suscripcion.class, subscriptionId);
            sub.actualizarDesdeStripe(sub.getStripeSubscriptionId(), sub.getPlan(), "canceled", null, null, false);
        });
        mvc.perform(get("/api/empresas").header("Authorization", collaborator)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TEAM_PRO_REQUIRED"));
        mvc.perform(post("/api/equipo/invitaciones").header("Authorization", admin).contentType("application/json").content("{\"empresaIds\":["+firstId+"]}")).andExpect(status().isForbidden());
        new TransactionTemplate(tm).executeWithoutResult(tx -> { var sub = em.find(Suscripcion.class, subscriptionId); sub.actualizarDesdeStripe(sub.getStripeSubscriptionId(), sub.getPlan(), "active", null, java.time.Instant.now().plusSeconds(86400), false); });
        mvc.perform(get("/api/empresas").header("Authorization", collaborator)).andExpect(status().isOk());
    }
    @Test void sixthActiveInviteIsRejectedGloballyAndMultiAssignmentCountsOnce() throws Exception {
        for (int i=0; i<5; i++) accept(createLink());
        mvc.perform(get("/api/equipo/invitaciones").header("Authorization", admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.invitadosActivos").value(5)).andExpect(jsonPath("$.puedeInvitar").value(false));
        mvc.perform(post("/api/equipo/invitaciones").header("Authorization", admin).contentType("application/json").content("{\"empresaIds\":["+firstId+"]}")).andExpect(status().isConflict());
    }
    @Test void revokedExpiredAndInvalidLinksCannotCreateAccounts() throws Exception {
        String raw = createLink();
        var list = mvc.perform(get("/api/equipo/invitaciones").header("Authorization", admin)).andReturn();
        Long id = json.readTree(list.getResponse().getContentAsString()).get("invitaciones").get(0).get("id").asLong();
        mvc.perform(delete("/api/equipo/invitaciones/"+id).header("Authorization", admin)).andExpect(status().isNoContent());
        for (String token : List.of(raw, "invalid")) mvc.perform(post("/api/auth/invitaciones/consultar").contentType("application/json").content(json.writeValueAsString(new com.vortise.gestion.application.dto.InvitacionesDto.Token(token)))).andExpect(status().isBadRequest());
        String expired = createLink();
        new TransactionTemplate(tm).executeWithoutResult(tx -> em.createNativeQuery("update invitaciones_equipo set vence_en = CURRENT_TIMESTAMP - INTERVAL '1' DAY where titular_id = :owner").setParameter("owner", ownerId).executeUpdate());
        mvc.perform(post("/api/auth/invitaciones/consultar").contentType("application/json").content(json.writeValueAsString(new com.vortise.gestion.application.dto.InvitacionesDto.Token(expired)))).andExpect(status().isBadRequest());
    }
    @Test void oldCreationAndRoleChangesCannotBypassInvitationPolicy() throws Exception {
        mvc.perform(post("/api/empresas/"+firstId+"/usuarios").header("Authorization", admin).contentType("application/json")
            .content(json.writeValueAsString(Map.of("email", "legacy-"+UUID.randomUUID()+"@example.invalid", "nombre", "Legacy", "rol", "ADMINISTRADOR", "password", PASSWORD))))
            .andExpect(status().isConflict());
        accept(createLink());
        mvc.perform(put("/api/empresas/"+firstId+"/usuarios/"+invitedId).header("Authorization", admin).contentType("application/json")
            .content(json.writeValueAsString(Map.of("nombre", "Invitado", "rol", "ADMINISTRADOR", "activo", true))))
            .andExpect(status().isBadRequest());
    }
    @Test void reactivationSharesGlobalSeatLimitAndRequiresPro() throws Exception {
        accept(createLink()); Long disabledId = invitedId;
        mvc.perform(put("/api/empresas/"+firstId+"/usuarios/"+disabledId).header("Authorization", admin).contentType("application/json")
            .content(json.writeValueAsString(Map.of("nombre", "Invitado", "rol", "COLABORADOR", "activo", false)))).andExpect(status().isOk());
        for (int i=0; i<5; i++) accept(createLink());
        mvc.perform(put("/api/empresas/"+secondId+"/usuarios/"+disabledId).header("Authorization", admin).contentType("application/json")
            .content(json.writeValueAsString(Map.of("nombre", "Invitado", "rol", "COLABORADOR", "activo", true)))).andExpect(status().isConflict());
        Boolean active = new TransactionTemplate(tm).execute(tx -> em.find(UsuarioEmpresa.class, disabledId).isActivo());
        assertFalse(active);
    }
    @Test void duplicateEmailRollsBackWithoutConsumingInvitation() throws Exception {
        String token = createLink();
        String email = new TransactionTemplate(tm).execute(tx -> em.find(UsuarioEmpresa.class, ownerId).getEmail());
        mvc.perform(post("/api/auth/invitaciones/aceptar").contentType("application/json")
            .content(json.writeValueAsString(new com.vortise.gestion.application.dto.InvitacionesDto.Aceptar(token, "Existing", email, PASSWORD))))
            .andExpect(status().isConflict());
        accept(token);
        mvc.perform(get("/api/equipo/invitaciones").header("Authorization", admin)).andExpect(status().isOk()).andExpect(jsonPath("$.invitadosActivos").value(1));
    }
    @Test void selectionMustBeNonemptyAndBelongToTheAdministrator() throws Exception {
        mvc.perform(post("/api/equipo/invitaciones").header("Authorization", admin).contentType("application/json").content("{\"empresaIds\":[]}")).andExpect(status().isBadRequest());
        new TransactionTemplate(tm).executeWithoutResult(tx -> em.find(Empresa.class, excludedId).setCreadoPorUsuarioId(null));
        mvc.perform(post("/api/equipo/invitaciones").header("Authorization", admin).contentType("application/json")
            .content("{\"empresaIds\":["+firstId+","+excludedId+"]}")).andExpect(status().isForbidden());
    }

    @Test void basicCannotInviteOrKeepCollaboratorSessionsActive() throws Exception {
        String collaborator = accept(createLink());
        new TransactionTemplate(tm).executeWithoutResult(tx -> {
            var existing = em.createQuery("select p from PlanSuscripcion p where p.codigo = 'BASIC'", PlanSuscripcion.class).getResultList();
            PlanSuscripcion basic;
            if (existing.isEmpty()) { basic = new PlanSuscripcion("BASIC", "Basic", 1, "usd", 0, 1, List.of()); em.persist(basic); }
            else basic = existing.get(0);
            var sub = em.find(Suscripcion.class, subscriptionId);
            sub.actualizarDesdeStripe(sub.getStripeSubscriptionId(), basic, "active", null, java.time.Instant.now().plusSeconds(86400), false);
        });
        mvc.perform(post("/api/equipo/invitaciones").header("Authorization", admin).contentType("application/json").content("{\"empresaIds\":["+firstId+"]}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/empresas").header("Authorization", collaborator)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TEAM_PRO_REQUIRED"));
        mvc.perform(get("/api/equipo/invitaciones").header("Authorization", admin)).andExpect(status().isOk()).andExpect(jsonPath("$.puedeInvitar").value(false));
    }

    @Test void companyProDoesNotCoverOtherCompaniesAndPartialExpiryPreservesRemainingAccess() throws Exception {
        Long firstSubscription = new TransactionTemplate(tm).execute(tx -> {
            var own = em.find(Suscripcion.class, subscriptionId);
            own.actualizarDesdeStripe(own.getStripeSubscriptionId(), own.getPlan(), "canceled", null, null, false);
            var companyPlan = Suscripcion.paraEmpresa(em.find(Empresa.class, firstId), "cus_company_fixture_"+UUID.randomUUID());
            companyPlan.actualizarDesdeStripe("sub_company_fixture_"+UUID.randomUUID(), own.getPlan(), "active", null, java.time.Instant.now().plusSeconds(86400), false);
            em.persist(companyPlan); em.flush(); return companyPlan.getId();
        });
        // One company subscription cannot grant the other selected company.
        mvc.perform(post("/api/equipo/invitaciones").header("Authorization", admin).contentType("application/json")
            .content("{\"empresaIds\":["+firstId+","+secondId+"]}")).andExpect(status().isForbidden());
        new TransactionTemplate(tm).executeWithoutResult(tx -> {
            var companyPlan = Suscripcion.paraEmpresa(em.find(Empresa.class, secondId), "cus_company_fixture_"+UUID.randomUUID());
            companyPlan.actualizarDesdeStripe("sub_company_fixture_"+UUID.randomUUID(), em.find(Suscripcion.class, subscriptionId).getPlan(), "active", null, java.time.Instant.now().plusSeconds(86400), false);
            em.persist(companyPlan);
        });
        String collaborator = accept(createLink());
        new TransactionTemplate(tm).executeWithoutResult(tx -> {
            var first = em.find(Suscripcion.class, firstSubscription);
            first.actualizarDesdeStripe(first.getStripeSubscriptionId(), first.getPlan(), "active", null, java.time.Instant.now().minusSeconds(3600), false);
        });
        mvc.perform(get("/api/empresas/"+firstId).header("Authorization", collaborator)).andExpect(status().isForbidden());
        mvc.perform(get("/api/empresas/"+secondId+"/obras/pagina").header("Authorization", collaborator)).andExpect(status().isOk());
        mvc.perform(get("/api/empresas").header("Authorization", collaborator)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(secondId));
        mvc.perform(get("/api/me").header("Authorization", collaborator)).andExpect(status().isOk()).andExpect(jsonPath("$.empresaId").value(secondId));
        new TransactionTemplate(tm).executeWithoutResult(tx -> {
            var first = em.find(Suscripcion.class, firstSubscription);
            first.actualizarDesdeStripe(first.getStripeSubscriptionId(), first.getPlan(), "active", null, java.time.Instant.now().plusSeconds(86400), false);
        });
        mvc.perform(get("/api/empresas").header("Authorization", collaborator)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

}
