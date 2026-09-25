package com.tokenrealty.auth.service;

import com.tokenrealty.auth.entity.User;
import com.tokenrealty.security.JwtTokenService;
import com.tokenrealty.security.TokenPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenService jwtTokenService;

    public String createAccessToken(UUID userId, String email, User.UserRole role, boolean serviceAccount) {
        return jwtTokenService.createAccessToken(userId, email, role.name(), serviceAccount);
    }

    public TokenPrincipal parseAccessToken(String token) {
        return jwtTokenService.parseAccessToken(token);
    }

    public long accessTokenTtlSeconds() {
        return jwtTokenService.accessTokenTtlSeconds();
    }

    public long refreshTokenTtlDays() {
        return jwtTokenService.refreshTokenTtlDays();
    }
}
