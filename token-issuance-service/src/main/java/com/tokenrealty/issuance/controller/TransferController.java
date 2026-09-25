package com.tokenrealty.issuance.controller;

import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.service.TransferService;
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
@RequestMapping("/v1/tokens/{contractId}/transfers")
@Tag(name = "Transfers", description = "Token transfers between investors")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all transfers for a token contract")
    public Page<TokenTransferResponse> listByContract(
            @PathVariable UUID contractId,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findByContract(contractId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer by id")
    public TokenTransferResponse getById(@PathVariable UUID contractId, @PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER') or hasRole('SERVICE')")
    @Operation(summary = "Execute token transfer — checks compliance, submits on-chain")
    public TokenTransferResponse transfer(
            @PathVariable UUID contractId,
            @Valid @RequestBody TransferRequest request) {
        return service.transfer(contractId, request);
    }
}