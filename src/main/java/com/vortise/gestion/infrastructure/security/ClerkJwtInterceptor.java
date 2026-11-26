package com.vortise.gestion.infrastructure.security;

import com.vortise.gestion.domain.model.RolEmpresa;
import com.vortise.gestion.domain.model.UsuarioEmpresa;
import com.vortise.gestion.application.service.JwtService;
import com.vortise.gestion.service.ClerkUserSyncService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ClerkJwtInterceptor extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ClerkJwtInterceptor.class);
    
    private final ClerkUserSyncService clerkSync;
    private final JwtService jwtService;
    
    @Value("${app.clerk.enabled:false}")
    private boolean clerkEnabled;
    
    @Value("${app.clerk.secret-key:}")
    private String clerkSecretKey;

    public ClerkJwtInterceptor(ClerkUserSyncService clerkSync, JwtService jwtService) {
        this.clerkSync = clerkSync;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if (!clerkEnabled || clerkSecretKey == null || clerkSecretKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract Bearer token from Authorization header
        String token = extractBearerToken(request);
        
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Create secret key from Clerk secret
            SecretKey key = new SecretKeySpec(clerkSecretKey.getBytes(StandardCharsets.UTF_8), 0, 
                clerkSecretKey.getBytes(StandardCharsets.UTF_8).length, "HmacSHA256");
            
            // Validate JWT with Clerk secret key
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            // Extract claims from Clerk JWT
            String clerkUserId = claims.getSubject(); // "user_xxx"
            String email = claims.get("email", String.class);
            
            if (clerkUserId == null || email == null) {
                log.debug("Missing required claims in Clerk JWT");
                filterChain.doFilter(request, response);
                return;
            }
            
            log.debug("Processing Clerk JWT for user: {} ({})", clerkUserId, email);
            
            // Synchronize user from Clerk
            UsuarioEmpresa usuario = clerkSync.sincronizarDesdeClerk(clerkUserId, email);
            
            if (usuario != null && usuario.isActivo()) {
                // Generate internal JWT for the system to work normally
                List<Long> obras = usuario.getRol() == RolEmpresa.JEFE_DE_OBRA
                    ? usuario.getObrasAsignadas().stream().sorted().toList()
                    : List.of();
                
                String internalJwt = jwtService.generate(
                    usuario.getId(),
                    usuario.getEmpresa().getId(),
                    usuario.getEmail(),
                    usuario.getRol().name(),
                    usuario.isSuperAdmin(),
                    usuario.getSessionVersion(),
                    obras
                );
                
                log.debug("Generated internal JWT for Clerk user: {}", clerkUserId);
                
                // Wrap request to replace Authorization header with internal JWT
                // so JwtAuthenticationFilter processes it normally
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("Authorization".equalsIgnoreCase(name)) {
                            return "Bearer " + internalJwt;
                        }
                        return super.getHeader(name);
                    }
                };
                
                filterChain.doFilter(wrappedRequest, response);
                return;
            } else {
                log.warn("User not found after Clerk sync: {} ({})", clerkUserId, email);
            }
            
        } catch (JwtException e) {
            // Invalid Clerk JWT - continue (may be traditional JWT or public endpoint)
            log.debug("Invalid Clerk JWT: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing Clerk JWT", e);
        }
        
        // Continue with original request (may be traditional JWT, may be public endpoint)
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

