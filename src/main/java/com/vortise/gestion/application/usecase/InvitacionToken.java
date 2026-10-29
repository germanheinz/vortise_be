package com.vortise.gestion.application.usecase;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
final class InvitacionToken {
    private static final SecureRandom RANDOM = new SecureRandom();
    private InvitacionToken() { }
    static String nuevo() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    static String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new com.vortise.gestion.application.exception.SolicitudInvalidaException("La invitación no está disponible.");
        }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
}
