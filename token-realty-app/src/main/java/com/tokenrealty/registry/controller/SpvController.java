package com.tokenrealty.registry.controller;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.SpvEntity;
import com.tokenrealty.registry.service.SpvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/buildings/{buildingId}/spv")
@RequiredArgsConstructor
@Tag(name = "SPV", description = "Special Purpose Vehicle management")
public class SpvController {

    private final SpvService spvService;

    @GetMapping
    @Operation(summary = "Get SPV details for a building")
    public SpvResponse getByBuilding(@PathVariable UUID buildingId) {
        return spvService.findByBuilding(buildingId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register an SPV for a building")
    public SpvResponse create(
            @PathVariable UUID buildingId,
            @Valid @RequestBody CreateSpvRequest request) {
        return spvService.create(buildingId, request);
    }

    @PatchMapping("/{spvId}/wallet")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set SPV blockchain wallet address")
    public SpvResponse setWallet(
            @PathVariable UUID buildingId,
            @PathVariable UUID spvId,
            @RequestParam String walletAddress) {
        return spvService.updateWalletAddress(spvId, walletAddress);
    }

    @PatchMapping("/{spvId}/kyc")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    @Operation(summary = "Mark SPV KYC as verified or rejected")
    public SpvResponse verifyKyc(
            @PathVariable UUID buildingId,
            @PathVariable UUID spvId,
            @RequestParam boolean verified) {
        return spvService.verifyKyc(spvId, verified);
    }

    @PatchMapping("/{spvId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update SPV status")
    public SpvResponse updateStatus(
            @PathVariable UUID buildingId,
            @PathVariable UUID spvId,
            @RequestParam SpvEntity.SpvStatus status) {
        return spvService.updateStatus(spvId, status);
    }
}