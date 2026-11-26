package com.vortise.gestion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.application.service.JwtService;
import com.vortise.gestion.application.service.SesionService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class SessionCookiesTest {
    private final JwtService jwt = new JwtService("test-only-signing-key-with-enough-length-1234567890", 900_000);
    private final SessionCookies cookies = new SessionCookies(jwt, true);

    @Test
    void accessCookieIsHttpOnlySecureStrictAndScopedToTheApi() {
        String cookie = cookies.access("jwt-value").toString();

        assertTrue(cookie.startsWith("gestion_at=jwt-value"));
        assertTrue(cookie.contains("HttpOnly") && cookie.contains("Secure") && cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/api") && cookie.contains("Max-Age=900"));
    }

    @Test
    void refreshCookieIsOnlySentToTheAuthEndpoints() {
        String cookie = cookies.refresh("refresh-value", 604800).toString();

        assertTrue(cookie.contains("Path=/api/auth") && cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Max-Age=604800"));
    }

    @Test
    void secureFlagCanBeDisabledForPlainHttpLocalRuns() {
        assertFalse(new SessionCookies(jwt, false).access("x").toString().contains("Secure"));
    }

    @Test
    void clearedCookiesExpireImmediatelyOnTheSamePath() {
        assertTrue(cookies.clearAccess().toString().contains("Max-Age=0") && cookies.clearAccess().toString().contains("Path=/api"));
        assertTrue(cookies.clearRefresh().toString().contains("Max-Age=0") && cookies.clearRefresh().toString().contains("Path=/api/auth"));
    }

    @Test
    void responseSetsBothCookiesAndNeverPutsATokenInTheBody() {
        var datos = new LoginResponseDto(null, 1L, 2L, "Empresa", "Nombre", "e@example.invalid", "ADMINISTRADOR", false, false, null, false, List.of());

        var response = cookies.respond(HttpStatus.CREATED, new SesionService.Sesion(datos, "access-jwt", "refresh-raw"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        List<String> setCookie = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertEquals(2, setCookie.size());
        assertTrue(setCookie.get(0).startsWith("gestion_at=access-jwt") && setCookie.get(1).startsWith("gestion_rt=refresh-raw"));
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertNull(response.getBody().token());
    }

    @Test
    void readsOnlyNonBlankCookiesByName() {
        var request = new MockHttpServletRequest();
        assertNull(SessionCookies.read(request, SessionCookies.ACCESS));
        request.setCookies(new Cookie("gestion_at", "  "), new Cookie("other", "v"), new Cookie("gestion_rt", "r"));

        assertNull(SessionCookies.read(request, SessionCookies.ACCESS));
        assertEquals("r", SessionCookies.read(request, SessionCookies.REFRESH));
    }
}
