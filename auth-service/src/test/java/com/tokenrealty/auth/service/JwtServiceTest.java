package com.tokenrealty.auth.service;

import com.tokenrealty.auth.config.JwtProperties;
import com.tokenrealty.auth.entity.User;
import com.tokenrealty.auth.security.TokenPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService unit tests")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-min-32-characters-long!!");
        properties.setIssuer("tokenrealty-auth-test");
        properties.setAccessTokenTtlMinutes(15);
        jwtService = new JwtService(properties);
    }

    @Test
    @DisplayName("creates and parses access token")
    void roundTripToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.createAccessToken(
                userId, "investor@tokenrealty.com", User.UserRole.INVESTOR, false);

        TokenPrincipal principal = jwtService.parseAccessToken(token);

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("investor@tokenrealty.com");
        assertThat(principal.role()).isEqualTo(User.UserRole.INVESTOR);
        assertThat(principal.serviceAccount()).isFalse();
    }
}
