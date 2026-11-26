package com.vortise.gestion.infrastructure.security.clerk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.application.exception.IdentidadNoVerificadaException;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClerkTokenVerifierTest {
    private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");
    private final KeyPair clerkKeys = Jwts.SIG.RS256.keyPair().build();
    private final KeyPair otherKeys = Jwts.SIG.RS256.keyPair().build();
    private String userJson = "{}";

    private static String b64(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ClerkTokenVerifier verifier() {
        RSAPublicKey key = (RSAPublicKey) clerkKeys.getPublic();
        String jwks = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"kid-1\",\"n\":\""
            + b64(key.getModulus()) + "\",\"e\":\"" + b64(key.getPublicExponent()) + "\"}]}";
        return new ClerkTokenVerifier(true, path -> path.equals("/jwks") ? jwks : userJson,
            List.of("http://localhost:3000"), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private String token(KeyPair keys, String kid, String sub, String azp, Instant expiresAt) {
        var builder = Jwts.builder().header().keyId(kid).and().subject(sub)
            .issuedAt(Date.from(NOW.minusSeconds(10))).expiration(Date.from(expiresAt));
        if (azp != null) builder.claim("azp", azp);
        return builder.signWith(keys.getPrivate(), Jwts.SIG.RS256).compact();
    }

    @Test
    void acceptsOnlyClerkSignedUnexpiredTokensForAnAllowedOrigin() {
        var verifier = verifier();
        Instant later = NOW.plus(Duration.ofMinutes(1));

        assertEquals("user_1", verifier.verifySession(token(clerkKeys, "kid-1", "user_1", "http://localhost:3000", later)));
        assertThrows(IdentidadNoVerificadaException.class, () -> verifier.verifySession(token(otherKeys, "kid-1", "user_1", null, later)));
        assertThrows(IdentidadNoVerificadaException.class, () -> verifier.verifySession(token(clerkKeys, "kid-1", "user_1", null, NOW.minusSeconds(60))));
        assertThrows(IdentidadNoVerificadaException.class, () -> verifier.verifySession(token(clerkKeys, "kid-1", "user_1", "https://evil.example", later)));
        assertThrows(IdentidadNoVerificadaException.class, () -> verifier.verifySession(token(clerkKeys, "unknown", "user_1", null, later)));
        assertThrows(IdentidadNoVerificadaException.class, () -> verifier.verifySession(token(clerkKeys, "kid-1", "org_1", null, later)));
        assertThrows(IdentidadNoVerificadaException.class, () -> verifier.verifySession(Jwts.builder().subject("user_1").compact()));
        assertThrows(IdentidadNoVerificadaException.class, () -> verifier.verifySession(null));
    }

    @Test
    void emailMustBeThePrimaryAndVerified() {
        var verifier = verifier();
        userJson = """
            {"primary_email_address_id":"e2","email_addresses":[
              {"id":"e1","email_address":"verified@example.invalid","verification":{"status":"verified"}},
              {"id":"e2","email_address":" Primary@Example.invalid ","verification":{"status":"verified"}}]}""";
        assertEquals(Optional.of("primary@example.invalid"), verifier.verifiedPrimaryEmail("user_1"));

        userJson = """
            {"primary_email_address_id":"e1","email_addresses":[
              {"id":"e1","email_address":"pending@example.invalid","verification":{"status":"unverified"}}]}""";
        assertEquals(Optional.empty(), verifier.verifiedPrimaryEmail("user_1"));
    }

    @Test
    void aDisabledVerifierRejectsEverything() {
        var disabled = new ClerkTokenVerifier(false, path -> "{}", List.of(), Clock.systemUTC());
        assertThrows(FeatureUnavailableException.class, () -> disabled.verifySession("token"));
    }
}
