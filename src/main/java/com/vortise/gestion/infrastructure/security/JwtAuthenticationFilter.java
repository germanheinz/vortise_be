package com.vortise.gestion.infrastructure.security;

import com.vortise.gestion.application.usecase.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final com.vortise.gestion.domain.repository.UsuarioEmpresaRepository usuarios;

    public JwtAuthenticationFilter(JwtService jwtService, com.vortise.gestion.domain.repository.UsuarioEmpresaRepository usuarios) {
        this.jwtService = jwtService; this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(authorization.substring(7));
                Number userId = claims.get("userId", Number.class);
                Number version = claims.get("sessionVersion", Number.class);
                var usuario = userId == null || version == null ? null : usuarios.findById(userId.longValue()).orElse(null);
                if (usuario == null || !usuario.isActivo() || usuario.getSessionVersion() != version.longValue()) {
                    throw new IllegalArgumentException("Invalid session");
                }
                var authorities = new java.util.ArrayList<SimpleGrantedAuthority>(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
                if (usuario.isSuperAdmin()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                }
                var authentication = new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, authorities);
                authentication.setDetails(io.jsonwebtoken.Jwts.claims().add(claims)
                    .add("userId", usuario.getId()).add("empresaId", usuario.getEmpresa().getId())
                    .add("rol", usuario.getRol().name()).add("superAdmin", usuario.isSuperAdmin()).build());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
