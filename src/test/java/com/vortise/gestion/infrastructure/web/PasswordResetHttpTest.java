package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vortise.gestion.application.port.PasswordMailGateway;
import com.vortise.gestion.application.usecase.*;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.infrastructure.config.*;
import com.vortise.gestion.domain.repository.PasswordResetTokenRepository;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordResetHttpTest {
    private static final String BASE = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String URL = BASE + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY = signingKey();
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
    @Autowired UsuarioEmpresaRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordResetTransactions resets;
    @Autowired JwtService jwt;
    @MockBean PasswordMailGateway mail;
    @MockBean DataSeeder dataSeeder;
    @MockBean UsuarioDemoSeeder demoSeeder;
    @MockBean PlanSuscripcionSeeder planSeeder;
    @MockBean SuperAdminSeeder adminSeeder;
    private Long userId;
    private long initialVersion;
    private String bearer;
    private final String email = "password-reset@example.invalid";
    private final String oldPassword = "synthetic-old-pass";
    @BeforeEach void fixtures() throws Exception {
        try (var c = source.getConnection()) { assertEquals(BASE, c.getMetaData().getURL().split(";", 2)[0]); }
        jdbc.update("delete from password_reset_tokens"); jdbc.update("delete from password_reset_quotas");
        jdbc.update("delete from usuarios_empresa"); jdbc.update("delete from empresas");
        jdbc.update("insert into password_reset_quotas (clave, inicio, solicitudes) values ('global', ?, 0)", Instant.now());
        new TransactionTemplate(tm).executeWithoutResult(s -> {
            var company = new Empresa("Reset isolated", null); em.persist(company);
            var user = new UsuarioEmpresa(company, email, "Reset test", encoder.encode(oldPassword), RolEmpresa.CONSULTA);
            em.persist(user); em.flush(); userId = user.getId(); initialVersion = user.getSessionVersion();
            bearer = jwt.generate(userId, company.getId(), email, "CONSULTA", false, user.getSessionVersion());
        });
        reset(mail); when(mail.enabled()).thenReturn(true);
    }
    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private String issue() throws Exception {
        mvc.perform(post("/api/auth/password-reset/request").contentType("application/json").content(json(Map.of("email", email))))
            .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control", "no-store"));
        var url = ArgumentCaptor.forClass(String.class); verify(mail).sendReset(eq(email), url.capture());
        return url.getValue().split("#token=", 2)[1];
    }
    private org.springframework.test.web.servlet.ResultActions confirm(String token, String password) throws Exception {
        return mvc.perform(post("/api/auth/password-reset/confirm").contentType("application/json").content(json(Map.of("token", token, "newPassword", password))));
    }
    @Test void resetIsHashedSingleUseAndRevokesOldSession() throws Exception {
        String raw = issue(); assertEquals(43, raw.length());
        var stored = tokens.findById(userId).orElseThrow(); assertEquals(PasswordResetTransactions.hash(raw), stored.getTokenHash()); assertNotEquals(raw, stored.getTokenHash());
        assertTrue(stored.getVenceEn().isBefore(Instant.now().plusSeconds(1801)));
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + bearer)).andExpect(status().isOk());
        confirm(raw, "synthetic-new-pass").andExpect(status().isNoContent());
        assertTrue(encoder.matches("synthetic-new-pass", users.findById(userId).orElseThrow().getPasswordHash()));
        verify(mail).sendChanged(email);
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + bearer)).andExpect(status().isUnauthorized());
        confirm(raw, "synthetic-another-pass").andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType("application/json").content(json(Map.of("email", email, "password", oldPassword)))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json").content(json(Map.of("email", email, "password", "synthetic-new-pass")))).andExpect(status().isOk());
    }
    @Test void publicRequestDoesNotEnumerateAndAccountCooldownReusesNoToken() throws Exception {
        String body = mvc.perform(post("/api/auth/password-reset/request").contentType("application/json").content(json(Map.of("email", "unknown@example.invalid")))).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        verify(mail, never()).sendReset(anyString(), anyString());
        String raw = issue();
        mvc.perform(post("/api/auth/password-reset/request").contentType("application/json").content(json(Map.of("email", email))))
            .andExpect(status().isAccepted()).andExpect(content().json(body));
        verify(mail, times(1)).sendReset(anyString(), anyString()); assertEquals(PasswordResetTransactions.hash(raw), tokens.findById(userId).orElseThrow().getTokenHash());
    }
    @Test void expiredAndReplacedTokensAreRejected() throws Exception {
        String first = issue();
        jdbc.update("update password_reset_tokens set vence_en = ?", Instant.now().minusSeconds(1));
        confirm(first, "synthetic-new-pass").andExpect(status().isBadRequest());
        jdbc.update("update password_reset_quotas set inicio = ?", Instant.now().minusSeconds(3600));
        clearInvocations(mail); String replacement = issue(); assertNotEquals(first, replacement);
        confirm(first, "synthetic-new-pass").andExpect(status().isBadRequest());
        confirm(replacement, "synthetic-new-pass").andExpect(status().isNoContent());
    }
    @Test void ipLimitAppliesToUnknownAddressesToo() throws Exception {
        for (int i = 0; i < 5; i++) mvc.perform(post("/api/auth/password-reset/request").contentType("application/json").content(json(Map.of("email", "unknown" + i + "@example.invalid")))).andExpect(status().isAccepted());
        mvc.perform(post("/api/auth/password-reset/request").contentType("application/json").content(json(Map.of("email", email)))).andExpect(status().isAccepted());
        verify(mail, never()).sendReset(anyString(), anyString()); assertTrue(tokens.findById(userId).isEmpty());
    }
    @Test void mailFailureAndDisabledMailRemainGeneric() throws Exception {
        doThrow(new IllegalStateException("sensitive provider detail")).when(mail).sendReset(anyString(), anyString());
        mvc.perform(post("/api/auth/password-reset/request").contentType("application/json").content(json(Map.of("email", email)))).andExpect(status().isAccepted()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(email))));
        when(mail.enabled()).thenReturn(false); clearInvocations(mail);
        mvc.perform(post("/api/auth/password-reset/request").contentType("application/json").content(json(Map.of("email", email)))).andExpect(status().isAccepted());
        verify(mail, never()).sendReset(anyString(), anyString());
    }
    @Test void twoConcurrentConfirmationsOnlyChangePasswordOnce() throws Exception {
        String raw = issue(); var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        Callable<Boolean> task = () -> { start.await(); try { resets.confirm(raw, "synthetic-new-pass"); return true; } catch (com.vortise.gestion.application.exception.SolicitudInvalidaException e) { return false; } };
        try {
            var a = pool.submit(task); var b = pool.submit(task); start.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(initialVersion + 1, users.findById(userId).orElseThrow().getSessionVersion());
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS)); }
    }
    @Test void inactiveRoleChangesAndLegacyTokensDoNotKeepAuthority() throws Exception {
        mvc.perform(get("/api/admin/pagos").header("Authorization", "Bearer " + bearer)).andExpect(status().isForbidden());
        new TransactionTemplate(tm).executeWithoutResult(s -> { var u = users.findById(userId).orElseThrow(); u.setRol(RolEmpresa.RESPONSABLE); users.save(u); });
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + bearer)).andExpect(status().isUnauthorized());
        var u = users.findById(userId).orElseThrow(); String current = jwt.generate(userId, u.getEmpresa().getId(), email, "RESPONSABLE", false, u.getSessionVersion());
        new TransactionTemplate(tm).executeWithoutResult(s -> { var active = users.findById(userId).orElseThrow(); active.setActivo(false); users.save(active); });
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + current)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer broken")).andExpect(status().isUnauthorized());
        var legacy = io.jsonwebtoken.Jwts.builder().subject(email).claim("userId", userId).expiration(new Date(System.currentTimeMillis() + 60000)).signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + legacy)).andExpect(status().isUnauthorized());
    }
    @Test void globalLimitIsPersistentAcrossDifferentEmailsAndIps() throws Exception {
        for (int i = 0; i < 60; i++) {
            final int suffix = i;
            mvc.perform(post("/api/auth/password-reset/request").with(request -> { request.setRemoteAddr("192.0.2." + suffix); return request; })
                .contentType("application/json").content(json(Map.of("email", "quota" + i + "@example.invalid")))).andExpect(status().isAccepted());
        }
        mvc.perform(post("/api/auth/password-reset/request").with(request -> { request.setRemoteAddr("198.51.100.1"); return request; })
            .contentType("application/json").content(json(Map.of("email", email)))).andExpect(status().isAccepted());
        verify(mail, never()).sendReset(anyString(), anyString());
    }
    @Test void longUtf8PasswordDoesNotConsumeTokenAndNotificationFailureDoesNotUndoChange() throws Exception {
        String raw = issue(); confirm(raw, "é".repeat(40)).andExpect(status().isBadRequest());
        doThrow(new IllegalStateException("private SMTP detail")).when(mail).sendChanged(anyString());
        confirm(raw, "synthetic-new-pass").andExpect(status().isNoContent());
        assertTrue(encoder.matches("synthetic-new-pass", users.findById(userId).orElseThrow().getPasswordHash()));
        confirm(raw, "synthetic-another-pass").andExpect(status().isBadRequest());
    }
}
