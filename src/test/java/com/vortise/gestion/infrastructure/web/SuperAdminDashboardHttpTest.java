package com.vortise.gestion.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vortise.gestion.application.usecase.JwtService;
import com.vortise.gestion.infrastructure.config.DataSeeder;
import com.vortise.gestion.infrastructure.config.PlanSuscripcionSeeder;
import com.vortise.gestion.infrastructure.config.SuperAdminSeeder;
import com.vortise.gestion.infrastructure.config.UsuarioDemoSeeder;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("planta-authorization-test")
class SuperAdminDashboardHttpTest {
    private static final String DATABASE_URL = "jdbc:h2:mem:planta_auth_" + UUID.randomUUID();
    private static final String JDBC_URL = DATABASE_URL + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String JWT_SECRET = generateTestSigningKey();
    private static final List<String> ENDPOINTS = List.of("/api/admin/dashboard", "/api/admin/empresas", "/api/admin/usuarios",
        "/api/admin/suscripciones", "/api/admin/pagos", "/api/admin/pagos/configuracion");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @MockBean private DataSeeder dataSeeder;
    @MockBean private UsuarioDemoSeeder usuarioDemoSeeder;
    @MockBean private PlanSuscripcionSeeder planSuscripcionSeeder;
    @MockBean private SuperAdminSeeder superAdminSeeder;

    @DynamicPropertySource
    static void isolatedProperties(DynamicPropertyRegistry registry) {
        registry.add("app.test-isolation.datasource-url", () -> JDBC_URL);
        registry.add("app.test-isolation.jwt-secret", () -> JWT_SECRET);
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-expiration-ms", () -> 3_600_000L);
        registry.add("app.stripe.secret-key", () -> "");
        registry.add("app.stripe.webhook-secret", () -> "");
        registry.add("app.stripe.price-basic", () -> "");
        registry.add("app.stripe.price-pro", () -> "");
        registry.add("logging.level.org.hibernate.SQL", () -> "OFF");
        registry.add("logging.level.org.hibernate.orm.jdbc.bind", () -> "OFF");
    }

    @Test
    @SuppressWarnings("null")
    void allReadEndpointsRequireExactlyTheSuperAdminAuthority() throws Exception {
        for (String endpoint : ENDPOINTS) {
            mockMvc.perform(get(endpoint))
                .andExpect(status().isForbidden());
            mockMvc.perform(get(endpoint).header("Authorization", bearer(false)))
                .andExpect(status().isForbidden());
            for (String role : List.of("JEFE_DE_OBRA", "RESPONSABLE", "CONSULTA")) {
                String token = jwtService.generate(1L, 1L, "platform-test@example.invalid", role, false);
                mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            }
            mockMvc.perform(get(endpoint).header("Authorization", bearer(true)))
                .andExpect(status().isOk());
        }
    }

    @Test
    @SuppressWarnings("null")
    void pagedEndpointsReturnExplicitPaginationMetadata() throws Exception {
        for (String endpoint : ENDPOINTS.subList(1, ENDPOINTS.size() - 1)) {
            mockMvc.perform(get(endpoint).header("Authorization", bearer(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").exists());
        }
    }

    @Test
    void billingListsAcceptAbsentAndPresentOptionalFilters() throws Exception {
        for (String estado : List.of("", "AcTiVe")) {
            mockMvc.perform(get("/api/admin/suscripciones")
                    .param("estado", estado).param("empresaId", "42")
                    .header("Authorization", bearer(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        }
        for (String estado : List.of("", "PaGaDo")) {
            for (String desde : List.of("", "2026-10-01")) {
                for (String hasta : List.of("", "2026-10-27")) {
                    mockMvc.perform(get("/api/admin/pagos")
                            .param("estado", estado).param("desde", desde).param("hasta", hasta)
                            .header("Authorization", bearer(true)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalElements").value(0));
                }
            }
        }
    }

    private String bearer(boolean superAdmin) {
        return "Bearer " + jwtService.generate(1L, 1L, "platform-test@example.invalid", "ADMINISTRADOR", superAdmin);
    }

    private static String generateTestSigningKey() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
