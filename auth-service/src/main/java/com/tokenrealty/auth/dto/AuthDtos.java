package com.tokenrealty.auth.dto;

import com.tokenrealty.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Builder
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull User.UserRole role
    ) {
    }

    @Builder
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    @Builder
    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    @Builder
    public record ServiceTokenRequest(
            @NotBlank String clientId,
            @NotBlank String clientSecret
    ) {
    }

    @Builder
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            UUID userId,
            String email,
            User.UserRole role
    ) {
    }

    @Builder
    public record UserProfileResponse(
            UUID id,
            String email,
            User.UserRole role,
            String walletAddress,
            Instant createdAt
    ) {
    }

    @Builder
    public record UpdateWalletRequest(
            @NotBlank @Size(max = 66) String walletAddress
    ) {
    }
}
