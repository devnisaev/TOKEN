package com.tokenrealty.auth.service;

import com.tokenrealty.auth.entity.RefreshToken;
import com.tokenrealty.auth.entity.User;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issueRefreshToken(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plusSeconds(jwtService.refreshTokenTtlDays() * 86400))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional(readOnly = true)
    public UUID validateAndGetUserId(String rawToken) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hash(rawToken))
                .orElseThrow(() -> new ValidationException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("Refresh token expired");
        }
        return stored.getUserId();
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
