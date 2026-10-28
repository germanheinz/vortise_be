package com.vortise.gestion.infrastructure.web;

import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.domain.model.*;
import com.vortise.gestion.infrastructure.config.*;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("planta-authorization-test")
class MonitoringHttpTest {
    private static final String URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String KEY = signingKey();
    @DynamicPropertySource static void isolate(DynamicPropertyRegistry p) {
        p.add("app.test-isolation.datasource-url", () -> URL); p.add("spring.datasource.url", () -> URL);
        p.add("app.test-isolation.jwt-secret", () -> KEY); p.add("app.security.jwt-secret", () -> KEY);
        p.add("app.password-mail.enabled", () -> false);
        for (String key : java.util.List.of("secret-key", "webhook-secret", "price-basic", "price-pro")) p.add("app.stripe." + key, () -> "");
    }
    private static String signingKey() { byte[] key = new byte[64]; new SecureRandom().nextBytes(key); return Base64.getEncoder().encodeToString(key); }
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired PlatformTransactionManager tm;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
    @MockitoBean DataSeeder dataSeeder;
    @MockitoBean UsuarioDemoSeeder demoSeeder;
    @MockitoBean SuperAdminSeeder adminSeeder;
    @MockitoBean PlanSuscripcionSeeder planSeeder;
    @Autowired org.springframework.boot.health.registry.HealthContributorRegistry healthRegistry;
    private org.springframework.boot.health.contributor.HealthContributor originalDb;
    private Health simulatedDb;
    @BeforeEach void healthy() {
        simulatedDb = Health.up().withDetail("private", "synthetic-secret").build();
        originalDb = healthRegistry.unregisterContributor("db");
        org.junit.jupiter.api.Assertions.assertNotNull(originalDb);
        healthRegistry.registerContributor("db", (HealthIndicator) () -> simulatedDb);
    }
    @AfterEach void restoreDbContributor() {
        if (originalDb != null) {
            healthRegistry.unregisterContributor("db");
            healthRegistry.registerContributor("db", originalDb);
        }
    }

    @Test void publicHealthReturnsOnlyStateAndServerGeneratedCorrelation() throws Exception {
        for (String path : java.util.List.of("/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness")) {
            mvc.perform(get(path).header("X-Request-ID", "untrusted-id"))
                .andExpect(status().isOk()).andExpect(content().json("{\"status\":\"UP\"}"))
                .andExpect(jsonPath("$.components").doesNotExist()).andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(header().string("X-Request-ID", org.hamcrest.Matchers.matchesPattern("[a-f0-9-]{36}")));
        }
    }

    @Test void dependencyFailureStopsReadinessButNotLiveness() throws Exception {
        simulatedDb = Health.down().withDetail("private", "synthetic-secret").build();
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable())
            .andExpect(content().json("{\"status\":\"DOWN\"}"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test void metricsRequireSuperAdminAndOtherActuatorEndpointsRemainDenied() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics").header("Authorization", bearer(false))).andExpect(status().isForbidden());
        String admin = bearer(true);
        mvc.perform(get("/actuator/metrics").header("Authorization", admin)).andExpect(status().isOk()).andExpect(jsonPath("$.names").isArray());
        for (String path : java.util.List.of("/actuator/env", "/actuator/configprops", "/actuator/heapdump", "/actuator/health/db", "/actuator")) {
            mvc.perform(get(path).header("Authorization", admin)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/actuator/metrics").header("Authorization", admin)).andExpect(status().isForbidden());
    }

    private String bearer(boolean superAdmin) {
        return new TransactionTemplate(tm).execute(status -> {
            var company = new Empresa("Monitoring " + UUID.randomUUID(), null); em.persist(company); em.flush();
            return "Bearer " + HttpIdentityFixtures.token(em, jwt, company.getId(), RolEmpresa.ADMINISTRADOR, superAdmin);
        });
    }
}
