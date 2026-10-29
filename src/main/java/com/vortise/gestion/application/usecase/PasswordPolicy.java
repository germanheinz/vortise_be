package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import java.nio.charset.StandardCharsets;

/** BCrypt's limit is bytes, not only Java characters. Never includes the value in errors. */
public final class PasswordPolicy {
    private PasswordPolicy() { }
    public static void validate(String password) {
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new SolicitudInvalidaException("La contraseña debe tener entre 8 y 72 caracteres y no superar 72 bytes.");
        }
    }
}
