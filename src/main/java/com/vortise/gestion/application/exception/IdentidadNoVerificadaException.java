package com.vortise.gestion.application.exception;

/** The external identity credential is missing or invalid; reported as HTTP 401. */
public class IdentidadNoVerificadaException extends RuntimeException {
    public IdentidadNoVerificadaException(String message) {
        super(message);
    }
}
