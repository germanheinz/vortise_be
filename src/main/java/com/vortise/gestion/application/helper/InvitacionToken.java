package com.vortise.gestion.application.helper;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
/** One-time invitation links shared by user and team invitations. */
public final class InvitacionToken {
    /** Deliberately does not reveal whether the email already has an account elsewhere. */
    public static final String CUENTA_EXISTENTE = "No se pudo crear la cuenta con ese email. Iniciá sesión con tu cuenta existente.";
    private static final SecureRandom RANDOM = new SecureRandom();
    private InvitacionToken() { }
    public static String nuevo() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public static String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw noDisponible();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
    /** The token travels in the URL fragment so it never reaches server logs or Referer headers. */
    public static String enlace(String frontendUrl, String ruta, String parametro, String token) {
        return frontendUrl.replaceAll("/+$", "") + ruta + "#" + parametro + "=" + token;
    }
    public static SolicitudInvalidaException noDisponible() { return new SolicitudInvalidaException("La invitación no está disponible."); }
}

