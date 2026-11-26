package com.vortise.gestion.infrastructure.security;

import com.vortise.gestion.service.authentication.JwtService;
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
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final com.vortise.gestion.service.equipo.EquipoAccessPolicy equipo;
    private final com.vortise.gestion.repository.UsuarioEmpresaRepository usuarios;

    public JwtAuthenticationFilter(JwtService jwtService, com.vortise.gestion.repository.UsuarioEmpresaRepository usuarios, com.vortise.gestion.service.equipo.EquipoAccessPolicy equipo) {
        this.jwtService = jwtService; this.usuarios = usuarios; this.equipo = equipo;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        // The browser session travels in an HttpOnly cookie; a Bearer header is still accepted for API clients.
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7)
            : SessionCookies.read(request, SessionCookies.ACCESS);
        if (token != null) {
            try {
                Claims claims = jwtService.parse(token);
                Number userId = claims.get("userId", Number.class);
                Number version = claims.get("sessionVersion", Number.class);
                var usuario = userId == null || version == null ? null : usuarios.findById(userId.longValue()).orElse(null);
                if (usuario == null || !usuario.isActivo() || usuario.getSessionVersion() != version.longValue()) {
                    throw new IllegalArgumentException("Invalid session");
                }
                if (usuario.isColaborador() && !request.getRequestURI().startsWith("/api/auth/") && equipo.empresasPermitidas(usuario).isEmpty()) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(403); response.setCharacterEncoding("UTF-8"); response.setContentType("application/json");
                    response.getWriter().write("{\"status\":403,\"code\":\"TEAM_PRO_REQUIRED\",\"message\":\"El acceso del equipo está suspendido hasta recuperar Pro.\"}");
                    return;
                }
                // Authorities come from the signed token; a token whose role no longer matches the account is rejected.
                String tokenRole = claims.get("rol", String.class);
                if (tokenRole == null || !tokenRole.equals(usuario.getRol().name())) {
                    throw new IllegalArgumentException("Invalid session");
                }
                var authorities = new java.util.ArrayList<SimpleGrantedAuthority>(List.of(new SimpleGrantedAuthority("ROLE_" + tokenRole)));
                if (usuario.isSuperAdmin()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                }
                var authentication = new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, authorities);
                authentication.setDetails(io.jsonwebtoken.Jwts.claims().add(claims)
                    .add("userId", usuario.getId()).add("empresaId", usuario.getEmpresa().getId())
                    .add("rol", tokenRole).add("superAdmin", usuario.isSuperAdmin())
                    .add("obras", "JEFE_DE_OBRA".equals(tokenRole) ? obrasFrom(claims) : List.<Long>of()).build());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private static List<Long> obrasFrom(Claims claims) {
        Object raw = claims.get("obras");
        if (!(raw instanceof java.util.Collection<?> values)) return List.of();
        return values.stream().filter(Number.class::isInstance).map(value -> ((Number) value).longValue()).sorted().toList();
    }
}
