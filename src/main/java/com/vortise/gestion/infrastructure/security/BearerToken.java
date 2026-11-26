package com.vortise.gestion.infrastructure.security;

/** Extracts the credential from an {@code Authorization: Bearer ...} header value. */
public final class BearerToken {
    private static final String PREFIX = "Bearer ";

    private BearerToken() { }

    /** Returns the credential, or {@code null} when the header is absent or not a Bearer header. */
    public static String from(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) return null;
        String credential = authorizationHeader.substring(PREFIX.length()).trim();
        return credential.isEmpty() ? null : credential;
    }
}
