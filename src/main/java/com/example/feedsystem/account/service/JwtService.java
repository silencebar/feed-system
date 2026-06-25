package com.example.feedsystem.account.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("#{T(java.time.Duration).ofDays(${jwt.access-token-expire-days:1})}")
    private Duration accessTokenTtl;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret must be configured");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256");
        }
        secretKey = Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(Long accountId, String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);
        return Jwts.builder()
                .claim("account_id", accountId)
                .claim("username", username)
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
