package com.vortise.gestion.infrastructure.security;

import com.vortise.gestion.application.dto.LoginResponseDto;
import com.vortise.gestion.service.authentication.JwtService;
import com.vortise.gestion.service.authentication.RefreshTokenService;
import com.vortise.gestion.service.authentication.SesionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * The browser session lives in two HttpOnly, SameSite=Strict cookies so scripts can never read a token:
 * a short access token for the API and a rotating refresh token that is only sent to the auth endpoints.
 */
@Component
public class SessionCookies {
    public static final String ACCESS = "gestion_at";
    public static final String REFRESH = "gestion_rt";
    private static final String ACCESS_PATH = "/api";
    private static final String REFRESH_PATH = "/api/auth";

    private final JwtService jwt;
    private final boolean secure;

    public SessionCookies(JwtService jwt, @Value("${app.security.cookie.secure:true}") boolean secure) {
        this.jwt = jwt;
        this.secure = secure;
    }

    public ResponseCookie access(String token) {
        return build(ACCESS, token, ACCESS_PATH, Duration.ofMillis(jwt.getExpirationMs()));
    }

    public ResponseCookie refresh(String token, long ttlSeconds) {
        return build(REFRESH, token, REFRESH_PATH, Duration.ofSeconds(ttlSeconds));
    }

    /** Sets both cookies and returns only the non-sensitive session data in the body. */
    public ResponseEntity<LoginResponseDto> respond(HttpStatus status, SesionService.Sesion sesion) {
        return ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, access(sesion.accessToken()).toString())
            .header(HttpHeaders.SET_COOKIE, refresh(sesion.refreshToken(), RefreshTokenService.TTL_SEGUNDOS).toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(sesion.datos());
    }

    public ResponseEntity<Void> cleared(HttpStatus status) {
        return ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, clearAccess().toString())
            .header(HttpHeaders.SET_COOKIE, clearRefresh().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .build();
    }

    public ResponseCookie clearAccess() { return build(ACCESS, "", ACCESS_PATH, Duration.ZERO); }

    public ResponseCookie clearRefresh() { return build(REFRESH, "", REFRESH_PATH, Duration.ZERO); }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secure).sameSite("Strict").path(path).maxAge(maxAge).build();
    }

    public static String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) return cookie.getValue();
        }
        return null;
    }
}
