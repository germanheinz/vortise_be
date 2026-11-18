package com.vortise.gestion.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Default-deny for read-only roles: CONSULTA and RESPONSABLE can never write through the API,
 * regardless of whether an endpoint declares its own authorization.
 */
public class SoloLecturaWriteGuard extends OncePerRequestFilter {
    public static final String CODE = "ACCESO_SOLO_LECTURA";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> READ_ONLY_AUTHORITIES = Set.of("ROLE_CONSULTA", "ROLE_RESPONSABLE");
    private static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String API_PREFIX = "/api/";
    private static final String AUTH_PREFIX = "/api/auth/";
    // Own-profile actions and the Stripe webhook (authenticated by signature, not by user).
    private static final Set<String> EXEMPT_PATHS = Set.of("/api/me/foto", "/api/stripe/webhook");

    private final boolean enabled;

    public SoloLecturaWriteGuard(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !enabled
            || !WRITE_METHODS.contains(request.getMethod())
            || !path.startsWith(API_PREFIX)
            || path.startsWith(AUTH_PREFIX)
            || EXEMPT_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        if (isReadOnlyUser()) {
            com.vortise.gestion.application.audit.AuditLog.accessDenied(CODE, SecurityActor.userId(), request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"code\":\"" + CODE
                + "\",\"message\":\"Tu acceso es de solo lectura.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isReadOnlyUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) return false;
        boolean readOnly = false;
        for (var authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            if (SUPER_ADMIN.equals(name)) return false;
            if (READ_ONLY_AUTHORITIES.contains(name)) readOnly = true;
        }
        return readOnly;
    }
}
