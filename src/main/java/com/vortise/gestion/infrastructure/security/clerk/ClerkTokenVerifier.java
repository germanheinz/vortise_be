package com.vortise.gestion.infrastructure.security.clerk;

import com.vortise.gestion.application.exception.FeatureUnavailableException;
import com.vortise.gestion.infrastructure.config.WebEnvironmentConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.ProtectedHeader;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies Clerk session tokens (RS256, keys from the instance JWKS) and resolves the account's verified primary email
 * through the Clerk Backend API. Only the signed token is trusted for the user id; the email never comes from the client.
 */
@Component
public class ClerkTokenVerifier {
    /** Fetches a Clerk Backend API path with the secret key and returns the JSON body. */
    @FunctionalInterface
    public interface ClerkApi {
        String get(String path) throws IOException;
    }

    public record VerifiedClerkUser(String userId, String email) { }

    private static final Duration KEY_REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(5);

    private final boolean enabled;
    private final ClerkApi api;
    private final List<String> allowedOrigins;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper();
    private volatile Map<String, RSAPublicKey> keys = Map.of();
    private volatile Instant keysFetchedAt = Instant.EPOCH;

    @Autowired
    public ClerkTokenVerifier(@Value("${app.clerk.enabled:false}") boolean enabled,
            @Value("${app.clerk.secret-key:}") String secretKey,
            @Value("${app.clerk.api-url:https://api.clerk.com/v1}") String apiUrl,
            WebEnvironmentConfiguration web) {
        this(enabled && secretKey != null && !secretKey.isBlank(), httpApi(apiUrl, secretKey), web.allowedOrigins(), Clock.systemUTC());
    }

    ClerkTokenVerifier(boolean enabled, ClerkApi api, List<String> allowedOrigins, Clock clock) {
        this.enabled = enabled;
        this.api = api;
        this.allowedOrigins = allowedOrigins;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Returns the Clerk user id ({@code sub}) of a valid, unexpired session token issued for an allowed origin. */
    public String verifySession(String token) {
        requireEnabled();
        if (token == null || token.isBlank()) throw new ClerkVerificationException("Falta la sesión de la cuenta.");
        Claims claims;
        try {
            claims = Jwts.parser()
                .keyLocator(new LocatorAdapter<Key>() {
                    @Override protected Key locate(ProtectedHeader header) { return keyFor(header.getKeyId()); }
                })
                .clock(() -> java.util.Date.from(clock.instant()))
                .clockSkewSeconds(CLOCK_SKEW.toSeconds())
                .build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException invalid) {
            throw new ClerkVerificationException("La sesión de la cuenta no es válida.");
        }
        String userId = claims.getSubject();
        if (userId == null || !userId.startsWith("user_")) throw new ClerkVerificationException("La sesión de la cuenta no es válida.");
        String azp = claims.get("azp", String.class);
        if (azp != null && !allowedOrigins.contains(azp.replaceAll("/+$", ""))) {
            throw new ClerkVerificationException("La sesión de la cuenta no es válida.");
        }
        return userId;
    }

    /** Verifies the token and resolves the account's primary email, which Clerk must report as verified. */
    public VerifiedClerkUser verify(String token) {
        String userId = verifySession(token);
        return new VerifiedClerkUser(userId, verifiedPrimaryEmail(userId)
            .orElseThrow(() -> new ClerkVerificationException("Confirmá tu email antes de continuar.")));
    }

    public Optional<String> verifiedPrimaryEmail(String userId) {
        requireEnabled();
        JsonNode user;
        try {
            user = json.readTree(api.get("/users/" + URLEncoder.encode(userId, StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException unavailable) {
            throw new FeatureUnavailableException("No pudimos comprobar la cuenta. Intentá nuevamente en unos minutos.");
        }
        String primaryId = user.path("primary_email_address_id").asText("");
        for (JsonNode address : user.path("email_addresses")) {
            if (primaryId.equals(address.path("id").asText()) && "verified".equals(address.path("verification").path("status").asText())) {
                String email = address.path("email_address").asText("").trim().toLowerCase(Locale.ROOT);
                return email.isEmpty() ? Optional.empty() : Optional.of(email);
            }
        }
        return Optional.empty();
    }

    private Key keyFor(String keyId) {
        RSAPublicKey key = keyId == null ? null : keys.get(keyId);
        if (key != null) return key;
        // Clerk rotates keys; refresh on an unknown kid, but not more than once per interval.
        Instant now = clock.instant();
        if (Duration.between(keysFetchedAt, now).compareTo(KEY_REFRESH_INTERVAL) >= 0) {
            synchronized (this) {
                if (Duration.between(keysFetchedAt, now).compareTo(KEY_REFRESH_INTERVAL) >= 0) {
                    keys = fetchKeys();
                    keysFetchedAt = now;
                }
            }
            key = keyId == null ? null : keys.get(keyId);
        }
        if (key == null) throw new ClerkVerificationException("La sesión de la cuenta no es válida.");
        return key;
    }

    private Map<String, RSAPublicKey> fetchKeys() {
        try {
            Map<String, RSAPublicKey> fetched = new HashMap<>();
            for (Jwk<?> jwk : Jwks.setParser().build().parse(api.get("/jwks"))) {
                if (jwk instanceof PublicJwk<?> publicJwk && publicJwk.toKey() instanceof RSAPublicKey rsa && jwk.getId() != null) {
                    fetched.put(jwk.getId(), rsa);
                }
            }
            return Map.copyOf(fetched);
        } catch (IOException | RuntimeException unavailable) {
            throw new FeatureUnavailableException("No pudimos comprobar la cuenta. Intentá nuevamente en unos minutos.");
        }
    }

    private void requireEnabled() {
        if (!enabled) throw new FeatureUnavailableException("El registro con cuenta externa no está habilitado.");
    }

    private static ClerkApi httpApi(String apiUrl, String secretKey) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        String base = apiUrl.replaceAll("/+$", "");
        return path -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + secretKey)
                .header("Accept", "application/json")
                .GET().build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) throw new IOException("Clerk API responded " + response.statusCode());
                return response.body();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted calling Clerk API", interrupted);
            }
        };
    }
}
