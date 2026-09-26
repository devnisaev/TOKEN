package com.tokenrealty.auth.service;

import com.tokenrealty.auth.dto.AuthDtos.*;
import com.tokenrealty.auth.entity.ServiceAccount;
import com.tokenrealty.auth.entity.User;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.auth.repository.ServiceAccountRepository;
import com.tokenrealty.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final ServiceAccountRepository serviceAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (request.role() != User.UserRole.INVESTOR && request.role() != User.UserRole.TENANT) {
            throw new ValidationException("Public registration allowed for INVESTOR or TENANT only");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();
        User saved = userRepository.save(user);
        return buildTokenResponse(saved, false);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }
        return buildTokenResponse(user, false);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        UUID userId = refreshTokenService.validateAndGetUserId(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found for refresh token"));

        refreshTokenService.revoke(request.refreshToken());
        return buildTokenResponse(user, false);
    }

    @Transactional(readOnly = true)
    public TokenResponse serviceToken(ServiceTokenRequest request) {
        ServiceAccount account = serviceAccountRepository.findByClientIdAndEnabledTrue(request.clientId())
                .orElseThrow(() -> new ValidationException("Invalid client credentials"));

        if (!passwordEncoder.matches(request.clientSecret(), account.getSecretHash())) {
            throw new ValidationException("Invalid client credentials");
        }

        String accessToken = jwtService.createAccessToken(
                account.getId(),
                account.getClientId(),
                account.getRole(),
                true
        );

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(null)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.accessTokenTtlSeconds())
                .userId(account.getId())
                .email(account.getClientId())
                .role(account.getRole())
                .build();
    }

    private TokenResponse buildTokenResponse(User user, boolean serviceAccount) {
        String accessToken = jwtService.createAccessToken(
                user.getId(), user.getEmail(), user.getRole(), serviceAccount);
        String refreshToken = refreshTokenService.issueRefreshToken(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtService.accessTokenTtlSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
