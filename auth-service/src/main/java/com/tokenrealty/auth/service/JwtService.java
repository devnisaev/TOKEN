package com.tokenrealty.auth.service;

import com.tokenrealty.auth.config.JwtProperties;
import com.tokenrealty.auth.entity.User;
import com.tokenrealty.auth.security.TokenPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID userId, String email, User.UserRole role, boolean serviceAccount) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getAccessTokenTtlMinutes() * 60);
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("service", serviceAccount)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public TokenPrincipal parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new TokenPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                User.UserRole.valueOf(claims.get("role", String.class)),
                Boolean.TRUE.equals(claims.get("service", Boolean.class))
        );
    }

    public long accessTokenTtlSeconds() {
        return properties.getAccessTokenTtlMinutes() * 60;
    }

    public long refreshTokenTtlDays() {
        return properties.getRefreshTokenTtlDays();
    }
}
