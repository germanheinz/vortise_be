package com.vortise.gestion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieOriginGuardTest {
    private static final String APP = "https://app.example.invalid";
    private final CookieOriginGuard guard = new CookieOriginGuard(List.of(APP, "http://localhost:3000"));

    private record Result(MockHttpServletResponse response, MockFilterChain chain) {
        boolean passed() { return chain.getRequest() != null; }
    }

    private Result run(String method, String cookie, String origin, String referer, String authorization) throws Exception {
        var request = new MockHttpServletRequest(method, "/api/proyectos/7/tareas");
        request.setRequestURI("/api/proyectos/7/tareas");
        if (cookie != null) request.setCookies(new Cookie(cookie, "value"));
        if (origin != null) request.addHeader("Origin", origin);
        if (referer != null) request.addHeader("Referer", referer);
        if (authorization != null) request.addHeader("Authorization", authorization);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        guard.doFilter(request, response, chain);
        return new Result(response, chain);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void cookieWritesFromAnAllowedOriginPass(String method) throws Exception {
        assertTrue(run(method, SessionCookies.ACCESS, APP, null, null).passed());
        assertTrue(run(method, SessionCookies.REFRESH, "http://localhost:3000", null, null).passed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void cookieWritesFromAnotherOriginAreRejected(String method) throws Exception {
        var result = run(method, SessionCookies.ACCESS, "https://attacker.example.invalid", null, null);

        assertEquals(403, result.response().getStatus());
        assertTrue(result.response().getContentAsString().contains("\"code\":\"" + CookieOriginGuard.CODE + "\""));
        assertTrue(!result.passed());
    }

    @Test
    void cookieWritesWithoutOriginOrRefererAreRejected() throws Exception {
        assertEquals(403, run("POST", SessionCookies.ACCESS, null, null, null).response().getStatus());
    }

    @Test
    void refererIsUsedWhenThereIsNoOrigin() throws Exception {
        assertTrue(run("POST", SessionCookies.ACCESS, null, APP + "/proyectos/7", null).passed());
        assertEquals(403, run("POST", SessionCookies.ACCESS, null, "https://attacker.example.invalid/x", null).response().getStatus());
        assertEquals(403, run("POST", SessionCookies.ACCESS, null, "not a url", null).response().getStatus());
    }

    @Test
    void originLookalikesAreNotAccepted() throws Exception {
        for (String origin : List.of(APP + ".attacker.invalid", "http://app.example.invalid", APP + ":8443", "null")) {
            assertEquals(403, run("POST", SessionCookies.ACCESS, origin, null, null).response().getStatus(), origin);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
    void safeMethodsAreNeverChecked(String method) throws Exception {
        assertTrue(run(method, SessionCookies.ACCESS, "https://attacker.example.invalid", null, null).passed());
    }

    @Test
    void requestsWithoutSessionCookiesOrWithABearerHeaderAreNotChecked() throws Exception {
        assertTrue(run("POST", null, "https://attacker.example.invalid", null, null).passed());
        assertTrue(run("POST", SessionCookies.ACCESS, null, null, "Bearer token").passed());
    }
}
