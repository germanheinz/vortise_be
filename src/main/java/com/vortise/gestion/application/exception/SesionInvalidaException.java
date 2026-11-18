package com.vortise.gestion.application.exception;

/** The refresh token is missing, expired, revoked, reused or no longer matches the account; reported as HTTP 401. */
public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException() {
        super("La sesión expiró. Iniciá sesión de nuevo.");
    }
}
