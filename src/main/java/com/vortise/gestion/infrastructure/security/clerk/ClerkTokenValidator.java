package com.vortise.gestion.infrastructure.security.clerk;

import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.domain.repository.UsuarioEmpresaRepository;
import com.vortise.gestion.service.usuario.ClerkUserSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ClerkTokenValidator extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ClerkTokenValidator.class);
    private final UsuarioEmpresaRepository usuarioRepository;
    private final ClerkUserSyncService clerkSync;

    @Value("${app.clerk.enabled:false}")
    private boolean clerkEnabled;

    public ClerkTokenValidator(UsuarioEmpresaRepository usuarioRepository, ClerkUserSyncService clerkSync) {
        this.usuarioRepository = usuarioRepository;
        this.clerkSync = clerkSync;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Extract Clerk user ID from JWT claims (injected by Clerk middleware)
        String clerkUserId = (String) request.getAttribute("clerk_user_id");
        String clerkEmail = (String) request.getAttribute("clerk_email");
        
        if (clerkEnabled && clerkUserId != null) {
            try {
                // Sync user from Clerk
                UsuarioEmpresa usuario = clerkSync.sincronizarDesdeClerk(clerkUserId, clerkEmail);
                
                if (usuario != null) {
                    // Set as principal in SecurityContext
                    var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        usuario.getId(), null, null
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Clerk user synchronized: {}", clerkUserId);
                }
            } catch (Exception e) {
                log.error("Error synchronizing Clerk user", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
