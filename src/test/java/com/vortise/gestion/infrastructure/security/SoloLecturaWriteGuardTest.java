package com.vortise.gestion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SoloLecturaWriteGuardTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse run(boolean enabled, String method, String path, String... authorities) throws Exception {
        if (authorities.length > 0) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()));
        }
        var request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new SoloLecturaWriteGuard(enabled).doFilter(request, response, chain);
        if (response.getStatus() == HttpServletResponse.SC_OK && chain.getRequest() == null) {
            throw new AssertionError("Filter neither blocked nor continued the chain");
        }
        return response;
    }

    private static void assertContinued(MockHttpServletResponse response) {
        assertEquals(200, response.getStatus());
    }

    @ParameterizedTest
    @CsvSource({"CONSULTA,POST", "CONSULTA,PUT", "CONSULTA,PATCH", "CONSULTA,DELETE",
        "RESPONSABLE,POST", "RESPONSABLE,PUT", "RESPONSABLE,PATCH", "RESPONSABLE,DELETE"})
    void readOnlyRolesCannotWrite(String role, String method) throws Exception {
        var response = run(true, method, "/api/proyectos/7/tareas", "ROLE_" + role);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"" + SoloLecturaWriteGuard.CODE + "\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
    void readOnlyRolesCanRead(String method) throws Exception {
        assertContinued(run(true, method, "/api/proyectos/7/tareas", "ROLE_CONSULTA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMINISTRADOR", "ROLE_JEFE_DE_OBRA", "ROLE_COLABORADOR"})
    void writerRolesAreNotAffected(String authority) throws Exception {
        assertContinued(run(true, "POST", "/api/proyectos/7/tareas", authority));
    }

    @Test
    void superAdminIsNotAffectedEvenWithAReadOnlyRole() throws Exception {
        assertContinued(run(true, "DELETE", "/api/proyectos/7", "ROLE_CONSULTA", "ROLE_SUPER_ADMIN"));
    }

    @ParameterizedTest
    @CsvSource({"POST,/api/auth/login", "POST,/api/auth/password-reset/confirm", "POST,/api/stripe/webhook"})
    void exemptPathsStayAvailable(String method, String path) throws Exception {
        assertContinued(run(true, method, path, "ROLE_CONSULTA"));
    }

    @Test
    void guardOnlyAppliesUnderApiPrefix() throws Exception {
        assertContinued(run(true, "POST", "/actuator/health", "ROLE_CONSULTA"));
    }

    @Test
    void anonymousRequestsAreLeftToTheAuthorizationFilter() throws Exception {
        var response = run(true, "POST", "/api/proyectos/7/tareas");

        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void canBeDisabledByConfiguration() throws Exception {
        assertContinued(run(false, "POST", "/api/proyectos/7/tareas", "ROLE_CONSULTA"));
    }

    @Test
    void deniedResponseIsJsonWithoutLeakingInternalDetails() throws Exception {
        var response = run(true, "DELETE", "/api/proyectos/7", "ROLE_CONSULTA");

        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("{\"status\":403,\"error\":\"Forbidden\",\"code\":\"ACCESO_SOLO_LECTURA\",\"message\":\"Tu acceso es de solo lectura.\"}",
            response.getContentAsString());
    }
}
