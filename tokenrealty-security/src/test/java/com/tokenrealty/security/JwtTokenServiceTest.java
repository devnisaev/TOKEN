package com.tokenrealty.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenService unit tests")
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-min-32-characters-long!!");
        properties.setIssuer("tokenrealty-auth-test");
        jwtTokenService = new JwtTokenService(properties);
    }

    @Test
    @DisplayName("creates and parses access token")
    void roundTripToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenService.createAccessToken(
                userId, "investor@tokenrealty.com", UserRole.INVESTOR.name(), false);

        TokenPrincipal principal = jwtTokenService.parseAccessToken(token);

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("investor@tokenrealty.com");
        assertThat(principal.role()).isEqualTo(UserRole.INVESTOR);
        assertThat(principal.serviceAccount()).isFalse();
    }
}
