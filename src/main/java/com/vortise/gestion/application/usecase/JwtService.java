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

    public JwtService(@Value("${app.security.jwt-secret:dev-only-change-this-secret-key-1234567890}") String secret,
            @Value("${app.security.jwt-expiration-ms:86400000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(Long userId, Long empresaId, String email, String rol) {
        Date now = new Date();
        return Jwts.builder().subject(email).claim("userId", userId).claim("empresaId", empresaId).claim("rol", rol)
            .issuedAt(now).expiration(new Date(now.getTime() + expirationMs)).signWith(signingKey).compact();
    }

    public io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) signingKey).build().parseSignedClaims(token).getPayload();
    }
}
