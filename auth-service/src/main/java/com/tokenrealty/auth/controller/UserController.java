package com.tokenrealty.auth.controller;

import com.tokenrealty.auth.dto.AuthDtos.*;
import com.tokenrealty.security.TokenPrincipal;
import com.tokenrealty.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Authenticated user profile")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public UserProfileResponse me(@AuthenticationPrincipal TokenPrincipal principal) {
        return userService.getProfile(principal);
    }

    @PatchMapping("/me/wallet")
    @Operation(summary = "Link wallet address to current user")
    public UserProfileResponse updateWallet(
            @AuthenticationPrincipal TokenPrincipal principal,
            @Valid @RequestBody UpdateWalletRequest request) {
        return userService.updateWallet(principal, request);
    }
}
