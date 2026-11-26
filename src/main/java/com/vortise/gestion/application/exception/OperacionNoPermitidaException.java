package com.vortise.gestion.application.exception;

/** The current user may not perform this operation; reported as HTTP 403 without depending on the security framework. */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String message) {
        super(message);
    }
}
