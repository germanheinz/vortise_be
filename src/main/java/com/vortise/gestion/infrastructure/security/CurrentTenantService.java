package com.vortise.gestion.infrastructure.security;

import com.vortise.gestion.application.port.CurrentIdentity;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentTenantService implements CurrentIdentity {
    private final EmpresaRepository empresaRepository;
    private final com.vortise.gestion.domain.repository.UsuarioEmpresaRepository usuarios;
    private final com.vortise.gestion.application.usecase.EquipoAccessPolicy equipo;

    public CurrentTenantService(EmpresaRepository empresaRepository, com.vortise.gestion.domain.repository.UsuarioEmpresaRepository usuarios, com.vortise.gestion.application.usecase.EquipoAccessPolicy equipo) {
        this.empresaRepository = empresaRepository; this.usuarios = usuarios; this.equipo = equipo;
    }

    public Long empresaId() {
        if (isCollaborator()) return empresasAccesibles().stream().findFirst()
            .orElseThrow(() -> new AccessDeniedException("El equipo no tiene cobertura Pro activa"));
        Number empresaId = claims().get("empresaId", Number.class);
        if (empresaId == null) throw new IllegalStateException("El usuario no tiene empresa asignada");
        return empresaId.longValue();
    }

    public Long usuarioId() {
        Number usuarioId = claims().get("userId", Number.class);
        if (usuarioId == null) throw new IllegalStateException("El usuario no está identificado");
        return usuarioId.longValue();
    }

    public void requireEmpresa(Long empresaId) {
        if (!canAccessEmpresa(empresaId)) {
            throw new AccessDeniedException("No tienes acceso a esta empresa");
        }
    }

    public boolean canAccessEmpresa(Long empresaId) {
        if (isSuperAdmin()) return true;
        if (isCollaborator()) return empresasAccesibles().contains(empresaId);
        if (empresaId().equals(empresaId)) return true;
        if (!isAdministrator()) return false;
        Long usuarioId = usuarioId();
        return empresaRepository.findById(empresaId)
            .map(empresa -> usuarioId.equals(empresa.getCreadoPorUsuarioId()))
            .orElse(false);
    }

    public boolean isCollaborator() { return hasAuthority("ROLE_COLABORADOR"); }
    public java.util.List<Long> empresasAccesibles() {
        if (!isCollaborator()) return java.util.List.of(empresaId());
        return usuarios.findById(usuarioId()).map(u -> equipo.empresasPermitidas(u).stream().sorted().toList()).orElse(java.util.List.of());
    }

    public boolean isAdministrator() {
        return hasAuthority("ROLE_ADMINISTRADOR");
    }

    public boolean isSuperAdmin() {
        return hasAuthority("ROLE_SUPER_ADMIN");
    }

    private boolean hasAuthority(String authorityName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
            && !(authentication instanceof AnonymousAuthenticationToken)
            && authentication.getAuthorities().stream().anyMatch(authority -> authorityName.equals(authority.getAuthority()));
    }

    private Claims claims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !(authentication.getDetails() instanceof Claims claims)) {
            throw new IllegalStateException("No hay una empresa autenticada");
        }
        return claims;
    }
}
