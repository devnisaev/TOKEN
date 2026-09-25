package com.tokenrealty.issuance.controller;

import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.service.TokenIssuanceService;
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
@RequestMapping("/v1/tokens")
@Tag(name = "Token Issuance", description = "ERC-1400 token deployment and management")
public class TokenIssuanceController {

    private final TokenIssuanceService service;

    public TokenIssuanceController(TokenIssuanceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all token contracts")
    public Page<TokenContractResponse> listAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return activeOnly ? service.findAllActive(pageable) : service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get token contract by id")
    public TokenContractResponse getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/by-flat/{flatId}")
    @Operation(summary = "Get token contract by flat id")
    public TokenContractResponse getByFlatId(@PathVariable UUID flatId) {
        return service.findByFlatId(flatId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Issue tokens for a flat — deploys ERC-1400 contract on Polygon")
    public TokenContractResponse issueTokens(@Valid @RequestBody IssueTokenRequest request) {
        return service.issueTokens(request);
    }

    @PatchMapping("/{id}/enable-transfers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable token transfers (investors can trade)")
    public TokenContractResponse enableTransfers(@PathVariable UUID id) {
        return service.enableTransfers(id);
    }

    @PatchMapping("/{id}/suspend-transfers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend token transfers (trading halt)")
    public TokenContractResponse suspendTransfers(@PathVariable UUID id) {
        return service.suspendTransfers(id);
    }
}
