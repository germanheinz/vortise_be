package com.vortise.gestion.application.exception;

/** Too many failed login attempts for the account or the client address; reported as HTTP 429. */
public class DemasiadosIntentosException extends RuntimeException {
    public static final long REINTENTO_SEGUNDOS = 900;

    public DemasiadosIntentosException() {
        super("Demasiados intentos. Probá de nuevo en unos minutos.");
    }
}
