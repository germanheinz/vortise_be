package com.vortise.gestion.application.exception;

/** A verified identity that may not open a session; reported as HTTP 403 with a stable code. */
public class AccesoDenegadoException extends RuntimeException {
    public static final String CUENTA_NO_REGISTRADA = "CUENTA_NO_REGISTRADA";
    public static final String CUENTA_INACTIVA = "CUENTA_INACTIVA";

    private final String code;

    private AccesoDenegadoException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static AccesoDenegadoException cuentaNoRegistrada() {
        return new AccesoDenegadoException(CUENTA_NO_REGISTRADA,
            "No encontramos una cuenta de Vortise para este email. Registrá tu empresa o pedí una invitación.");
    }

    public static AccesoDenegadoException cuentaInactiva() {
        return new AccesoDenegadoException(CUENTA_INACTIVA, "Tu cuenta está desactivada. Consultá con el administrador.");
    }

    public String getCode() {
        return code;
    }
}
