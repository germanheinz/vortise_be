package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;
import com.vortise.gestion.application.usecase.PasswordResetTransactions;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.infrastructure.config.*;
import jakarta.persistence.EntityManager;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** New disposable tmpfs container15434 only. No existing payments15433 or hosted datasource. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("staging")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfSystemProperty(named = "auth.pg.enabled", matches = "true")
class PasswordResetPostgresTest {
    private static final String URL = "jdbc:postgresql://127.0.0.1:15434/auth008_test";
    @DynamicPropertySource static void isolation(DynamicPropertyRegistry p) throws Exception {
        var values = tools.jackson.databind.json.JsonMapper.builder().build().readTree(Files.readString(Path.of("/private/tmp/auth008-pg.json")));
        if (!URL.equals(values.path("url").asText()) || !"auth008_test".equals(values.path("username").asText())) throw new IllegalStateException("Disposable auth database required");
        byte[] bytes = new byte[64]; new SecureRandom().nextBytes(bytes); String key = Base64.getEncoder().encodeToString(bytes);
        p.add("spring.datasource.url", () -> URL); p.add("spring.datasource.username", () -> "auth008_test");
        p.add("spring.datasource.password", () -> values.path("password").asText());
        p.add("APP_DATABASE_JDBC_URL", () -> URL); p.add("APP_DATABASE_USERNAME", () -> "auth008_test");
        p.add("APP_DATABASE_PASSWORD", () -> values.path("password").asText());
        p.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        p.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        p.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); p.add("spring.flyway.enabled", () -> true);
        p.add("spring.sql.init.mode", () -> "never"); p.add("spring.h2.console.enabled", () -> false);
        p.add("app.security.jwt-secret", () -> key); p.add("app.password-mail.enabled", () -> false);
        p.add("app.superadmin.email", () -> ""); p.add("app.superadmin.password", () -> ""); p.add("app.demo.reset-enabled", () -> false);
        p.add("APP_ALLOWED_ORIGINS", () -> "https://auth008.example.invalid"); p.add("APP_FRONTEND_URL", () -> "https://auth008.example.invalid");
        for (String name : List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) p.add("app.stripe." + name, () -> "");
        p.add("logging.level.org.hibernate.SQL", () -> "OFF"); p.add("logging.level.org.hibernate.orm.jdbc.bind", () -> "OFF");
    }
    @MockitoBean DataSeeder dataSeeder;
    @MockitoBean UsuarioDemoSeeder demoSeeder;
    @MockitoBean PlanSuscripcionSeeder planSeeder;
    @MockitoBean SuperAdminSeeder adminSeeder;
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired UsuarioEmpresaRepository users;
    @Autowired PasswordResetTransactions resets;
    @Autowired PasswordEncoder encoder;
    @Test void migratesThroughV8ValidatesAndSerializesTokenConsumption() throws Exception {
        try (var c = source.getConnection()) { assertEquals(URL, c.getMetaData().getURL()); }
        assertEquals(8, jdbc.queryForObject("select count(*) from flyway_schema_history where success = true and version is not null", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from password_reset_quotas where clave = 'global'", Integer.class));
        String email = "pg-auth-" + UUID.randomUUID() + "@example.invalid";
        Long id = new TransactionTemplate(tm).execute(status -> {
            var company = new Empresa("Disposable auth PG", null); em.persist(company);
            var user = new UsuarioEmpresa(company, email, "PG auth", encoder.encode("old-synthetic-pass"), RolEmpresa.CONSULTA);
            em.persist(user); em.flush(); return user.getId();
        });
        long initialVersion = users.findById(id).orElseThrow().getSessionVersion();
        var issued = resets.issue(email, "192.0.2.1").orElseThrow();
        assertTrue(resets.issue(email, "192.0.2.2").isEmpty());
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        Callable<Boolean> consume = () -> {
            start.await(); try { resets.confirm(issued.token(), "new-synthetic-pass"); return true; }
            catch (com.vortise.gestion.application.exception.SolicitudInvalidaException ex) { return false; }
        };
        try {
            var a = pool.submit(consume); var b = pool.submit(consume); start.countDown();
            assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS)); }
        var user = users.findById(id).orElseThrow(); assertEquals(initialVersion + 1, user.getSessionVersion());
        assertTrue(encoder.matches("new-synthetic-pass", user.getPasswordHash()));
        assertEquals(1, jdbc.queryForObject("select count(*) from password_reset_tokens where usuario_id = ? and consumido_en is not null", Integer.class, id));
        // Entire tmpfs container is removed by the authorized harness; never clean any other database.
    }
}
