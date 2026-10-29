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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("production")
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "team15.pg.enabled", matches = "true")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class TeamInvitationsPostgresTest extends TeamInvitationsContract {
    private static final String URL = "jdbc:postgresql://127.0.0.1:15439/team15_test";
    private static final String USER = "team15_test";
    @DynamicPropertySource static void isolate(DynamicPropertyRegistry r) {
        String password = password();
        byte[] bytes = new byte[64]; new java.security.SecureRandom().nextBytes(bytes);
        String key = Base64.getEncoder().encodeToString(bytes);
        r.add("APP_DATABASE_JDBC_URL", () -> URL); r.add("APP_DATABASE_USERNAME", () -> USER);
        r.add("APP_DATABASE_PASSWORD", () -> password);
        r.add("spring.datasource.url", () -> URL); r.add("spring.datasource.username", () -> USER);
        r.add("spring.datasource.password", () -> password);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); r.add("spring.flyway.enabled", () -> true);
        r.add("spring.sql.init.mode", () -> "never"); r.add("spring.h2.console.enabled", () -> false);
        r.add("app.security.jwt-secret", () -> key); r.add("app.password-mail.enabled", () -> false);
        r.add("app.superadmin.email", () -> ""); r.add("app.superadmin.password", () -> "");
        r.add("app.demo.reset-enabled", () -> false); r.add("app.features.gastos-fijos-enabled", () -> false);
        r.add("APP_ALLOWED_ORIGINS", () -> "https://team15.example.invalid");
        r.add("APP_FRONTEND_URL", () -> "https://team15.example.invalid");
        for (String suffix : List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) r.add("app.stripe." + suffix, () -> "");
        r.add("logging.level.org.hibernate.SQL", () -> "OFF"); r.add("logging.level.org.hibernate.orm.jdbc.bind", () -> "OFF");
    }
    private static String password() {
        String value = System.getenv("TEAM15_PG_PASSWORD");
        if (!"true".equals(System.getProperty("team15.pg.enabled")) || value == null || value.length() < 40) {
            throw new IllegalStateException("Exclusive disposable quality database required");
        }
        return value;
    }
    @Override protected void requireOwned() throws Exception {
        try (var c = source.getConnection()) {
            assertEquals(URL, c.getMetaData().getURL()); assertEquals(USER, c.getMetaData().getUserName()); assertEquals("public", c.getSchema());
        }
    }
    @Test void concurrentLastSeatAcceptsOnlyOneDistinctAccount() throws Exception {
        for (int i=0; i<4; i++) accept(createLink());
        String first = createLink(), second = createLink();
        assertEquals(List.of(201, 409), race(first, second));
        long count = new TransactionTemplate(tm).execute(tx -> em.createQuery("select count(u) from UsuarioEmpresa u where u.invitanteId = :id and u.activo = true", Long.class).setParameter("id", ownerId).getSingleResult());
        assertEquals(5, count);
    }
    @Test void simultaneousUseConsumesOneInvitationOnly() throws Exception {
        String token = createLink();
        assertEquals(List.of(201, 400), race(token, token));
        long count = new TransactionTemplate(tm).execute(tx -> em.createQuery("select count(u) from UsuarioEmpresa u where u.invitanteId = :id", Long.class).setParameter("id", ownerId).getSingleResult());
        assertEquals(1, count);
    }
    @Test void flywayIncludesV10AndValidatesTeamTables() {
        Integer versions = new TransactionTemplate(tm).execute(tx -> ((Number)em.createNativeQuery("select count(*) from flyway_schema_history where success = true and version is not null", Integer.class).getSingleResult()).intValue());
        assertEquals(10, versions);
    }
    private List<Integer> race(String first, String second) throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (String token : List.of(first, second)) futures.add(pool.submit(() -> {
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Barrier timeout");
                var dto = new com.vortise.gestion.application.dto.InvitacionesDto.Aceptar(token, "Concurrent fixture", UUID.randomUUID()+"@example.invalid", PASSWORD);
                return mvc.perform(post("/api/auth/invitaciones/aceptar").contentType("application/json").content(json.writeValueAsString(dto))).andReturn().getResponse().getStatus();
            }));
            start.countDown();
            var result = new ArrayList<Integer>();
            for (var future : futures) result.add(future.get(30, java.util.concurrent.TimeUnit.SECONDS));
            Collections.sort(result); return result;
        } finally { start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)); }
    }
}
