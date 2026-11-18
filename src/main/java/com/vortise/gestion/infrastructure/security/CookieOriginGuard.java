package com.vortise.gestion.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF defence for cookie sessions: a state-changing request that carries one of our session cookies must
 * come from an allowed web origin. SameSite=Strict already keeps the cookies off cross-site requests; this
 * rejects the rest. Requests authenticated with a Bearer header (no cookie involved) are not affected.
 */
public class CookieOriginGuard extends OncePerRequestFilter {
    public static final String CODE = "ORIGEN_NO_PERMITIDO";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final List<String> allowedOrigins;

    public CookieOriginGuard(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) return true;
        if (request.getHeader("Authorization") != null) return true;
        return SessionCookies.read(request, SessionCookies.ACCESS) == null && SessionCookies.read(request, SessionCookies.REFRESH) == null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        if (allowed(request)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"code\":\"" + CODE + "\",\"message\":\"Origen no permitido.\"}");
    }

    private boolean allowed(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) origin = originOf(request.getHeader("Referer"));
        return origin != null && allowedOrigins.contains(origin);
    }

    private static String originOf(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = URI.create(url.trim());
            return uri.getScheme() == null || uri.getRawAuthority() == null ? null : uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
