package com.vortise.gestion.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentTenantService {
    public Long empresaId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !(authentication.getDetails() instanceof Claims claims)) {
            throw new IllegalStateException("No hay una empresa autenticada");
        }
        Number empresaId = claims.get("empresaId", Number.class);
        if (empresaId == null) throw new IllegalStateException("El usuario no tiene empresa asignada");
        return empresaId.longValue();
    }

    public void requireEmpresa(Long empresaId) {
        if (!isAdministrator() && !empresaId().equals(empresaId)) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes acceso a esta empresa");
        }
    }

    public boolean isAdministrator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
            && !(authentication instanceof AnonymousAuthenticationToken)
            && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMINISTRADOR".equals(authority.getAuthority()));
    }
}
