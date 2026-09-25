package com.tokenrealty.auth.service;

import com.tokenrealty.auth.dto.AuthDtos.LoginRequest;
import com.tokenrealty.auth.dto.AuthDtos.RegisterRequest;
import com.tokenrealty.auth.dto.AuthDtos.TokenResponse;
import com.tokenrealty.auth.entity.User;
import com.tokenrealty.auth.exception.ConflictException;
import com.tokenrealty.auth.exception.ValidationException;
import com.tokenrealty.auth.repository.ServiceAccountRepository;
import com.tokenrealty.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock ServiceAccountRepository serviceAccountRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("register creates investor and returns tokens")
    void registerSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@tokenrealty.com")
                .password("password123")
                .role(User.UserRole.INVESTOR)
                .build();

        User saved = User.builder()
                .email("new@tokenrealty.com")
                .role(User.UserRole.INVESTOR)
                .enabled(true)
                .build();
        saved.setId(UUID.randomUUID());

        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.createAccessToken(any(), anyString(), any(), any(Boolean.class))).thenReturn("access");
        when(refreshTokenService.issueRefreshToken(saved)).thenReturn("refresh");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);

        TokenResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    @DisplayName("register rejects admin role")
    void registerRejectsAdmin() {
        RegisterRequest request = RegisterRequest.builder()
                .email("bad@tokenrealty.com")
                .password("password123")
                .role(User.UserRole.ADMIN)
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("register rejects duplicate email")
    void registerDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("x@y.com")).thenReturn(true);

        RegisterRequest request = RegisterRequest.builder()
                .email("x@y.com")
                .password("password123")
                .role(User.UserRole.INVESTOR)
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("login validates password")
    void loginInvalidPassword() {
        User user = User.builder()
                .email("a@b.com")
                .passwordHash("hash")
                .role(User.UserRole.INVESTOR)
                .enabled(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong")))
                .isInstanceOf(ValidationException.class);
    }
}
