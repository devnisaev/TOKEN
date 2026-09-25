package com.tokenrealty.auth.service;

import com.tokenrealty.auth.dto.AuthDtos.*;
import com.tokenrealty.auth.entity.User;
import com.tokenrealty.auth.exception.ResourceNotFoundException;
import com.tokenrealty.auth.repository.UserRepository;
import com.tokenrealty.auth.security.TokenPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(TokenPrincipal principal) {
        if (principal.serviceAccount()) {
            throw new ResourceNotFoundException("Service accounts have no user profile");
        }
        return toProfile(getUser(principal.userId()));
    }

    @Transactional
    public UserProfileResponse updateWallet(TokenPrincipal principal, UpdateWalletRequest request) {
        if (principal.serviceAccount()) {
            throw new ResourceNotFoundException("Service accounts cannot set wallet");
        }
        User user = getUser(principal.userId());
        user.setWalletAddress(request.walletAddress());
        return toProfile(user);
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private static UserProfileResponse toProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .walletAddress(user.getWalletAddress())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
