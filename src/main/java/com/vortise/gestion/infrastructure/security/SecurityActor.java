package com.vortise.gestion.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.context.SecurityContextHolder;

/** Identity of the authenticated caller for audit entries; never exposes the email. */
public final class SecurityActor {
    private SecurityActor() { }

    public static Long userId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Claims claims
                && claims.get("userId") instanceof Number id) {
            return id.longValue();
        }
        return null;
    }
}
