package com.jsmc.spacemap.project.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:86400000}") long expirationMillis) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalArgumentException("app.security.jwt.secret must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(key);
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload().getSubject());
    }

    public String extractRole(String token) {
        Object role = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload().get("role");
        return role == null ? null : role.toString();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
