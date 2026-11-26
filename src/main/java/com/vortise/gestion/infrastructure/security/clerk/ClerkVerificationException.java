package com.vortise.gestion.infrastructure.security.clerk;

/** A Clerk session token or account could not be verified; reported as HTTP 401 without the underlying detail. */
public class ClerkVerificationException extends RuntimeException {
    public ClerkVerificationException(String message) {
        super(message);
    }
}
