package com.vortise.gestion.service.authentication;

import com.vortise.gestion.application.exception.LoginConContrasenaDesactivadoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Single switch for every endpoint that accepts a local password; only test profiles turn it on. */
@Component
public class LoginConContrasenaPolicy {
    private final boolean enabled;

    public LoginConContrasenaPolicy(@Value("${app.auth.password-login.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void requireEnabled() {
        if (!enabled) throw new LoginConContrasenaDesactivadoException();
    }
}
