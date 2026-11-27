package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.exception.IdentidadNoVerificadaException;
import com.vortise.gestion.application.port.IdentidadExternaVerificador;
import com.vortise.gestion.application.port.IdentidadExternaVerificador.IdentidadVerificada;
import com.vortise.gestion.infrastructure.config.PlanSuscripcionSeeder;
import com.vortise.gestion.infrastructure.config.SuperAdminSeeder;
import com.vortise.gestion.model.Empresa;
import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.UsuarioEmpresa;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP contract of the external-identity endpoints, with the Clerk verifier replaced by a test double and the
 * local password login switched off as in production. Runs only against a generated in-memory H2 database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
class ClerkAuthHttpTest {
    private static final String URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY = signingKey();
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @DynamicPropertySource static void isolate(DynamicPropertyRegistry p) {
        p.add("app.test-isolation.datasource-url", () -> URL); p.add("spring.datasource.url", () -> URL);
        p.add("app.test-isolation.jwt-secret", () -> KEY); p.add("app.security.jwt-secret", () -> KEY);
        p.add("app.password-mail.enabled", () -> false);
        p.add("app.auth.password-login.enabled", () -> false);
        for (String key : List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) p.add("app.stripe." + key, () -> "");
    }
    private static String signingKey() { byte[] key = new byte[64]; new SecureRandom().nextBytes(key); return Base64.getEncoder().encodeToString(key); }

    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager tm;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
    @MockitoBean SuperAdminSeeder adminSeeder;
    @MockitoBean PlanSuscripcionSeeder planSeeder;
    @MockitoBean IdentidadExternaVerificador verificador;

    @BeforeEach void rejectUnknownCredentials() {
        when(verificador.verificar(any())).thenThrow(new IdentidadNoVerificadaException("Credencial inválida"));
    }

    private void accept(String token, String subject, String email) {
        doReturn(new IdentidadVerificada(subject, email)).when(verificador).verificar(token);
    }

    private String existingUserEmail() {
        return new TransactionTemplate(tm).execute(status -> {
            var company = new Empresa("Clerk " + UUID.randomUUID(), null); em.persist(company); em.flush();
            String email = "existing-" + UUID.randomUUID() + "@example.invalid";
            em.persist(new UsuarioEmpresa(company, email, "Existing User", "not-a-login-password", RolEmpresa.ADMINISTRADOR)); em.flush();
            return email;
        });
    }

    private static String bearer(String token) { return "Bearer " + token; }

    @Test void sessionLinksExistingUserByVerifiedEmailAndSetsCookiesOnly() throws Exception {
        String email = existingUserEmail();
        String subject = "user_" + UUID.randomUUID();
        accept("tok-a", subject, email.toUpperCase());
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/auth/clerk/sesion").header(HttpHeaders.AUTHORIZATION, bearer("tok-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(cookie().httpOnly("gestion_at", true))
                .andExpect(cookie().httpOnly("gestion_rt", true))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        }
    }

    @Test void sessionRejectsMissingOrInvalidCredentialWith401() throws Exception {
        mvc.perform(post("/api/auth/clerk/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/clerk/sesion").header(HttpHeaders.AUTHORIZATION, "Basic abc")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/clerk/sesion").header(HttpHeaders.AUTHORIZATION, bearer("forged"))).andExpect(status().isUnauthorized());
    }

    @Test void sessionDeniesAccountWithoutRegisteredUser() throws Exception {
        accept("tok-unknown", "user_" + UUID.randomUUID(), "nobody-" + UUID.randomUUID() + "@example.invalid");
        mvc.perform(post("/api/auth/clerk/sesion").header(HttpHeaders.AUTHORIZATION, bearer("tok-unknown")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CUENTA_NO_REGISTRADA"))
            .andExpect(cookie().doesNotExist("gestion_at"));
    }

    @Test void sessionDeniesUserAlreadyLinkedToAnotherExternalAccount() throws Exception {
        String email = existingUserEmail();
        accept("tok-first", "user_" + UUID.randomUUID(), email);
        mvc.perform(post("/api/auth/clerk/sesion").header(HttpHeaders.AUTHORIZATION, bearer("tok-first"))).andExpect(status().isOk());
        accept("tok-second", "user_" + UUID.randomUUID(), email);
        mvc.perform(post("/api/auth/clerk/sesion").header(HttpHeaders.AUTHORIZATION, bearer("tok-second")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CUENTA_NO_REGISTRADA"));
    }

    @Test void registrationCreatesCompanyAdministratorFromVerifiedIdentityAndRejectsRepeats() throws Exception {
        String email = "owner-" + UUID.randomUUID() + "@example.invalid";
        accept("tok-register", "user_" + UUID.randomUUID(), email);
        String body = "{\"empresaNombre\":\"Constructora " + UUID.randomUUID() + "\",\"nombre\":\"Ana Test\",\"email\":\"attacker@example.invalid\"}";
        mvc.perform(post("/api/auth/clerk/registro").header(HttpHeaders.AUTHORIZATION, bearer("tok-register")).contentType(JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
            .andExpect(cookie().httpOnly("gestion_at", true));
        mvc.perform(post("/api/auth/clerk/registro").header(HttpHeaders.AUTHORIZATION, bearer("tok-register")).contentType(JSON)
                .content("{\"empresaNombre\":\"Otra " + UUID.randomUUID() + "\",\"nombre\":\"Ana Test\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void registrationRequiresCredentialAndValidBody() throws Exception {
        mvc.perform(post("/api/auth/clerk/registro").contentType(JSON).content("{\"empresaNombre\":\"X\",\"nombre\":\"Y\"}"))
            .andExpect(status().isUnauthorized());
        accept("tok-valid", "user_" + UUID.randomUUID(), "valid-" + UUID.randomUUID() + "@example.invalid");
        mvc.perform(post("/api/auth/clerk/registro").header(HttpHeaders.AUTHORIZATION, bearer("tok-valid")).contentType(JSON)
                .content("{\"empresaNombre\":\"\",\"nombre\":\"Y\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void invitationAcceptanceWithIdentityRequiresCredential() throws Exception {
        mvc.perform(post("/api/auth/invitaciones/aceptar-clerk").contentType(JSON).content("{\"token\":\"t\",\"nombre\":\"N\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/invitaciones-usuario/aceptar-clerk").contentType(JSON).content("{\"codigo\":\"c\",\"nombre\":\"N\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void passwordEndpointsAnswerGoneWhenLocalPasswordLoginIsOff() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(JSON).content("{\"email\":\"a@example.invalid\",\"password\":\"whatever-123\"}"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.code").value("LOGIN_CON_CONTRASENA_DESACTIVADO"));
    }
}
