package com.vortise.gestion.application.exception;

/** The action is allowed for the role in general but not for the current state of the resource; reported as HTTP 403. */
public class ReglaDeEstadoException extends OperacionNoPermitidaException {
    public static final String CODE = "REGLA_DE_ESTADO";

    public ReglaDeEstadoException(String message) {
        super(message);
    }
}
