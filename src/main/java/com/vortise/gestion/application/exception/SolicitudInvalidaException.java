package com.vortise.gestion.application.exception;

/** Client error that must be reported as HTTP 400 with a safe message. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String message) { super(message); }
}
