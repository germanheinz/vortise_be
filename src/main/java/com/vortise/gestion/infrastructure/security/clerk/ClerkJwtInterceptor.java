package com.vortise.gestion.infrastructure.security.clerk;

import com.vortise.gestion.model.RolEmpresa;
import com.vortise.gestion.model.UsuarioEmpresa;
import com.vortise.gestion.service.authentication.JwtService;
import com.vortise.gestion.service.usuario.ClerkUserSyncService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Accepts a Clerk session token as a Bearer credential: once Clerk's signature is verified and the account maps to an
 * existing active user, the request continues with an internal JWT so JwtAuthenticationFilter applies the usual rules.
 * Anything else (internal JWTs, invalid tokens, unknown accounts) passes through untouched.
 */
@Component
public class ClerkJwtInterceptor extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ClerkJwtInterceptor.class);

    private final ClerkUserSyncService clerkSync;
    private final ClerkTokenVerifier verifier;
    private final JwtService jwtService;

    public ClerkJwtInterceptor(ClerkUserSyncService clerkSync, ClerkTokenVerifier verifier, JwtService jwtService) {
        this.clerkSync = clerkSync;
        this.verifier = verifier;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (!verifier.isEnabled() || token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String clerkUserId = verifier.verifySession(token);
            // Only look up the email (a Clerk API call) for accounts not linked yet.
            UsuarioEmpresa usuario = clerkSync.buscarPorClerkUserId(clerkUserId)
                .orElseGet(() -> verifier.verifiedPrimaryEmail(clerkUserId)
                    .map(email -> clerkSync.sincronizarDesdeClerk(clerkUserId, email)).orElse(null));

            if (usuario != null && usuario.isActivo()) {
                List<Long> obras = usuario.getRol() == RolEmpresa.JEFE_DE_OBRA
                    ? usuario.getObrasAsignadas().stream().sorted().toList()
                    : List.of();
                String internalJwt = jwtService.generate(usuario.getId(), usuario.getEmpresa().getId(), usuario.getEmail(),
                    usuario.getRol().name(), usuario.isSuperAdmin(), usuario.getSessionVersion(), obras);

                // Replace the Authorization header so JwtAuthenticationFilter processes the internal JWT normally.
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("Authorization".equalsIgnoreCase(name)) return "Bearer " + internalJwt;
                        return super.getHeader(name);
                    }
                };
                filterChain.doFilter(wrappedRequest, response);
                return;
            }
            log.debug("No active local user for Clerk account {}", clerkUserId);
        } catch (ClerkVerificationException notClerk) {
            // Not a valid Clerk token: may be an internal JWT or a public endpoint.
            log.debug("Bearer token is not a valid Clerk session");
        } catch (RuntimeException e) {
            log.warn("Could not process Clerk session: {}", e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
