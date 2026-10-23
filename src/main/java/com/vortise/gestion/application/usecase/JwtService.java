package com.vortise.gestion.application.usecase;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final Key signingKey;
    private final long expirationMs;

    public JwtService(@Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.security.jwt-secret must be explicitly configured with at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(Long userId, Long empresaId, String email, String rol) {
        return generate(userId, empresaId, email, rol, false);
    }

    public String generate(Long userId, Long empresaId, String email, String rol, boolean superAdmin) {
        Date now = new Date();
        return Jwts.builder().subject(email).claim("userId", userId).claim("empresaId", empresaId).claim("rol", rol)
            .claim("superAdmin", superAdmin)
            .issuedAt(now).expiration(new Date(now.getTime() + expirationMs)).signWith(signingKey).compact();
    }

    public io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) signingKey).build().parseSignedClaims(token).getPayload();
    }
}
