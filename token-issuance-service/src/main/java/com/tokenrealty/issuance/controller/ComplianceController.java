package com.tokenrealty.issuance.controller;

import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/compliance")
@Tag(name = "Compliance", description = "KYC whitelist management for investors")
public class ComplianceController {

    private final ComplianceService service;

    public ComplianceController(ComplianceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    @Operation(summary = "List all compliance records")
    public Page<ComplianceRecordResponse> listAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    @Operation(summary = "Get compliance record by id")
    public ComplianceRecordResponse getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/investor/{investorId}")
    @Operation(summary = "Get compliance record by investor id")
    public ComplianceRecordResponse getByInvestor(@PathVariable UUID investorId) {
        return service.findByInvestor(investorId);
    }

    @GetMapping("/check/{walletAddress}")
    @Operation(summary = "Check if a wallet is KYC whitelisted (DB + on-chain)")
    public ComplianceCheckResponse checkWallet(@PathVariable String walletAddress) {
        return service.checkWallet(walletAddress);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    @Operation(summary = "Register an investor for KYC compliance")
    public ComplianceRecordResponse register(
            @Valid @RequestBody RegisterComplianceRequest request) {
        return service.register(request);
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    @Operation(summary = "Mark investor as KYC verified and whitelist on-chain")
    public ComplianceRecordResponse verify(
            @PathVariable UUID id,
            @Valid @RequestBody ComplianceVerifyRequest request) {
        return service.verify(id, request);
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    @Operation(summary = "Revoke investor KYC and remove from whitelist")
    public ComplianceRecordResponse revoke(
            @PathVariable UUID id,
            @RequestParam String reason) {
        return service.revoke(id, reason);
    }
}
