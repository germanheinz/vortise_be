package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.infrastructure.config.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/** Browser session in HttpOnly cookies: login, cookie auth, rotation, reuse detection, CSRF origin check, logout. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CookieSessionHttpTest {
    private static final String BASE = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String URL = BASE + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY = signingKey();
    private static final String ORIGIN = "http://localhost:3000";
    private static final String EMAIL = "cookie-session@example.invalid";
    private static final String PASSWORD = "synthetic-cookie-pass";

    @DynamicPropertySource static void isolation(DynamicPropertyRegistry p) {
        p.add("app.test-isolation.datasource-url", () -> URL); p.add("spring.datasource.url", () -> URL);
        p.add("app.test-isolation.jwt-secret", () -> KEY); p.add("app.security.jwt-secret", () -> KEY);
        p.add("app.password-mail.enabled", () -> false);
        p.add("logging.level.org.hibernate.SQL", () -> "OFF");
        p.add("logging.level.org.hibernate.orm.jdbc.bind", () -> "OFF");
        for (String name : List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) p.add("app.stripe." + name, () -> "");
    }
    private static String signingKey() { byte[] b = new byte[64]; new SecureRandom().nextBytes(b); return Base64.getEncoder().encodeToString(b); }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired PasswordEncoder encoder;
    @MockitoBean PlanSuscripcionSeeder planSeeder;
    @MockitoBean SuperAdminSeeder adminSeeder;
    private Long userId;
    private Long companyId;

    @BeforeEach void fixtures() throws Exception {
        try (var c = source.getConnection()) { assertEquals(BASE, c.getMetaData().getURL().split(";", 2)[0]); }
        jdbc.update("delete from refresh_tokens"); jdbc.update("delete from password_reset_quotas");
        jdbc.update("delete from usuario_obra"); jdbc.update("delete from invitacion_usuario_obras"); jdbc.update("delete from invitaciones_usuario"); jdbc.update("delete from proyectos");
        jdbc.update("delete from usuarios_empresa"); jdbc.update("delete from empresas");
        jdbc.update("insert into password_reset_quotas (clave, inicio, solicitudes) values ('global', ?, 0)", Instant.now());
        new TransactionTemplate(tm).executeWithoutResult(s -> {
            var company = new Empresa("Cookie isolated", null); em.persist(company);
            var user = new UsuarioEmpresa(company, EMAIL, "Cookie test", encoder.encode(PASSWORD), RolEmpresa.ADMINISTRADOR);
            em.persist(user); em.flush(); userId = user.getId(); companyId = company.getId();
        });
    }

    private MvcResult login() throws Exception {
        return mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("email", EMAIL, "password", PASSWORD)))).andExpect(status().isOk()).andReturn();
    }
    private static String cookie(MvcResult result, String name) {
        return result.getResponse().getHeaders("Set-Cookie").stream().filter(c -> c.startsWith(name + "=")).findFirst().orElseThrow();
    }
    private static String value(String setCookie) { return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';')); }

    @Test void loginSetsHttpOnlyCookiesAndKeepsTokensOutOfTheBody() throws Exception {
        var result = login();
        var body = mapper.readTree(result.getResponse().getContentAsString());

        assertTrue(body.path("token").isNull(), "the body must not carry a token");
        assertEquals("ADMINISTRADOR", body.path("rol").asText());
        String access = cookie(result, "gestion_at"), refresh = cookie(result, "gestion_rt");
        for (String c : List.of(access, refresh)) assertTrue(c.contains("HttpOnly") && c.contains("Secure") && c.contains("SameSite=Strict"), c);
        assertTrue(access.contains("Path=/api") && access.contains("Max-Age=900"));
        assertTrue(refresh.contains("Path=/api/auth"));
        assertEquals("no-store", result.getResponse().getHeader("Cache-Control"));
    }

    @Test void theAccessCookieAuthenticatesRequests() throws Exception {
        var access = new Cookie("gestion_at", value(cookie(login(), "gestion_at")));

        mvc.perform(get("/api/me").cookie(access)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL));
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").cookie(new Cookie("gestion_at", "forged"))).andExpect(status().isUnauthorized());
    }

    @Test void cookieWritesNeedAnAllowedOrigin() throws Exception {
        var access = new Cookie("gestion_at", value(cookie(login(), "gestion_at")));

        mvc.perform(delete("/api/me/foto").cookie(access)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ORIGEN_NO_PERMITIDO"));
        // A foreign Origin is already refused by the CORS layer (plain-text 403) before it reaches the origin guard.
        mvc.perform(delete("/api/me/foto").cookie(access).header("Origin", "https://attacker.example.invalid"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/me/foto").cookie(access).header("Origin", ORIGIN)).andExpect(status().is2xxSuccessful());
    }

    @Test void refreshRotatesTheTokenAndIssuesANewAccessCookie() throws Exception {
        var first = value(cookie(login(), "gestion_rt"));

        var rotated = mvc.perform(post("/api/auth/refresh").cookie(new Cookie("gestion_rt", first)).header("Origin", ORIGIN))
            .andExpect(status().isOk()).andReturn();

        assertTrue(mapper.readTree(rotated.getResponse().getContentAsString()).path("token").isNull());
        assertNotEquals(first, value(cookie(rotated, "gestion_rt")));
        var access = new Cookie("gestion_at", value(cookie(rotated, "gestion_at")));
        mvc.perform(get("/api/me").cookie(access)).andExpect(status().isOk());
    }

    @Test void refreshWithoutAnOriginOrWithoutACookieIsRejected() throws Exception {
        var first = value(cookie(login(), "gestion_rt"));

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("gestion_rt", first))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/refresh").header("Origin", ORIGIN)).andExpect(status().isUnauthorized());
    }

    @Test void reusingARetiredRefreshTokenRevokesTheWholeChain() throws Exception {
        var first = value(cookie(login(), "gestion_rt"));
        var second = value(cookie(mvc.perform(post("/api/auth/refresh").cookie(new Cookie("gestion_rt", first)).header("Origin", ORIGIN))
            .andExpect(status().isOk()).andReturn(), "gestion_rt"));
        // Make the retirement of the first token older than the grace period for concurrent tabs.
        jdbc.update("update refresh_tokens set usado_en = ? where usado_en is not null", Instant.now().minusSeconds(120));

        var replay = mvc.perform(post("/api/auth/refresh").cookie(new Cookie("gestion_rt", first)).header("Origin", ORIGIN))
            .andExpect(status().isUnauthorized()).andReturn();

        assertTrue(cookie(replay, "gestion_at").contains("Max-Age=0") && cookie(replay, "gestion_rt").contains("Max-Age=0"));
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("gestion_rt", second)).header("Origin", ORIGIN)).andExpect(status().isUnauthorized());
    }

    @Test void aSessionVersionChangeEndsTheRefreshChain() throws Exception {
        var first = value(cookie(login(), "gestion_rt"));
        jdbc.update("update usuarios_empresa set session_version = session_version + 1 where id = ?", userId);

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("gestion_rt", first)).header("Origin", ORIGIN)).andExpect(status().isUnauthorized());
    }

    @Test void logoutClearsTheCookiesAndEndsTheChain() throws Exception {
        var result = login();
        var refresh = new Cookie("gestion_rt", value(cookie(result, "gestion_rt")));

        var out = mvc.perform(post("/api/auth/logout").cookie(refresh).header("Origin", ORIGIN)).andExpect(status().isNoContent()).andReturn();

        assertTrue(cookie(out, "gestion_at").contains("Max-Age=0") && cookie(out, "gestion_rt").contains("Max-Age=0"));
        mvc.perform(post("/api/auth/refresh").cookie(refresh).header("Origin", ORIGIN)).andExpect(status().isUnauthorized());
    }

    private static String tokenOf(String link) { return link.substring(link.indexOf("#token=") + 7); }

    @Test void anAdministratorInvitesAJefeWhoJoinsWithTheRoleAndSitesAndTheLinkWorksOnce() throws Exception {
        var admin = new Cookie("gestion_at", value(cookie(login(), "gestion_at")));
        Long siteId = new TransactionTemplate(tm).execute(s -> {
            var site = new Proyecto(); site.setNombre("Obra invitada"); site.setEmpresaRelacionada(em.find(Empresa.class, companyId));
            em.persist(site); em.flush(); return site.getId();
        });
        var invites = "/api/empresas/" + companyId + "/usuarios/invitaciones";
        var created = mvc.perform(post(invites).cookie(admin).header("Origin", ORIGIN).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("email", "Jefe.Nuevo@Example.Invalid", "rol", "JEFE_DE_OBRA", "obraIds", List.of(siteId)))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("jefe.nuevo@example.invalid")).andReturn();
        String token = tokenOf(mapper.readTree(created.getResponse().getContentAsString()).path("enlace").asText());
        assertEquals(43, token.length());

        mvc.perform(post("/api/auth/invitaciones-usuario/consultar").contentType("application/json").content(mapper.writeValueAsString(Map.of("token", token))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("JEFE_DE_OBRA")).andExpect(jsonPath("$.empresaNombre").value("Cookie isolated"));
        var accepted = mvc.perform(post("/api/auth/invitaciones-usuario/aceptar").contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("token", token, "nombre", "Juan Jefe", "password", "synthetic-jefe-pass"))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("JEFE_DE_OBRA")).andReturn();

        assertTrue(mapper.readTree(accepted.getResponse().getContentAsString()).path("token").isNull());
        assertTrue(cookie(accepted, "gestion_at").contains("HttpOnly") && cookie(accepted, "gestion_rt").contains("HttpOnly"));
        var jefe = new Cookie("gestion_at", value(cookie(accepted, "gestion_at")));
        mvc.perform(get("/api/me").cookie(jefe)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("jefe.nuevo@example.invalid"))
            .andExpect(jsonPath("$.rol").value("JEFE_DE_OBRA")).andExpect(jsonPath("$.obras[0]").value(siteId));
        // The link is consumed; the new jefe can neither reuse it nor invite anyone.
        mvc.perform(post("/api/auth/invitaciones-usuario/aceptar").contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("token", token, "nombre", "Otra", "password", "synthetic-jefe-pass"))))
            .andExpect(status().isBadRequest());
        mvc.perform(post(invites).cookie(jefe).header("Origin", ORIGIN).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("email", "otro@example.invalid", "rol", "CONSULTA")))).andExpect(status().isForbidden());
        // The new account can sign in with its own password.
        mvc.perform(post("/api/auth/login").contentType("application/json").content(mapper.writeValueAsString(Map.of("email", "jefe.nuevo@example.invalid", "password", "synthetic-jefe-pass"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("JEFE_DE_OBRA"));
    }

    @Test void invitationsRejectUnofferedRolesForeignSitesAndRevokedOrUnknownLinks() throws Exception {
        var admin = new Cookie("gestion_at", value(cookie(login(), "gestion_at")));
        var invites = "/api/empresas/" + companyId + "/usuarios/invitaciones";
        mvc.perform(post(invites).cookie(admin).header("Origin", ORIGIN).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("email", "x@example.invalid", "rol", "COLABORADOR")))).andExpect(status().isBadRequest());
        mvc.perform(post(invites).cookie(admin).header("Origin", ORIGIN).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("email", "x@example.invalid", "rol", "JEFE_DE_OBRA", "obraIds", List.of(987654))))).andExpect(status().isBadRequest());
        mvc.perform(post(invites).cookie(admin).header("Origin", ORIGIN).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("email", EMAIL, "rol", "CONSULTA")))).andExpect(status().isConflict());

        var created = mvc.perform(post(invites).cookie(admin).header("Origin", ORIGIN).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("email", "revoked@example.invalid", "rol", "CONSULTA")))).andExpect(status().isCreated()).andReturn();
        var body = mapper.readTree(created.getResponse().getContentAsString());
        mvc.perform(delete(invites + "/" + body.path("id").asLong()).cookie(admin).header("Origin", ORIGIN)).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/invitaciones-usuario/aceptar").contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("token", tokenOf(body.path("enlace").asText()), "nombre", "X", "password", "synthetic-pass-1")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/invitaciones-usuario/consultar").contentType("application/json").content("{\"token\":\"not-a-token\"}")).andExpect(status().isBadRequest());
        mvc.perform(get(invites).cookie(admin)).andExpect(status().isOk()).andExpect(jsonPath("$[0].estado").value("REVOCADA"));
    }

    @Test void aBearerHeaderStillWorksForApiClientsWithoutAnOrigin() throws Exception {
        var jwt = value(cookie(login(), "gestion_at"));

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + jwt)).andExpect(status().isOk());
        mvc.perform(delete("/api/me/foto").header("Authorization", "Bearer " + jwt)).andExpect(status().is2xxSuccessful());
    }
}
