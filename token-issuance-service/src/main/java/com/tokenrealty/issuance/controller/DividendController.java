package com.tokenrealty.issuance.controller;

import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.service.DividendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@Tag(name = "Dividends", description = "Rental income distribution to token holders")
public class DividendController {

    private final DividendService service;

    public DividendController(DividendService service) {
        this.service = service;
    }

    @GetMapping("/tokens/{contractId}/dividends")
    @Operation(summary = "List all dividend payments for a token contract")
    public Page<DividendPaymentResponse> listByContract(
            @PathVariable UUID contractId,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findByContract(contractId, pageable);
    }

    @GetMapping("/investors/{investorId}/dividends")
    @Operation(summary = "List all dividend payments for an investor")
    public List<DividendPaymentResponse> listByInvestor(@PathVariable UUID investorId) {
        return service.findByInvestor(investorId);
    }

    @PostMapping("/tokens/{contractId}/dividends/distribute")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Distribute rental income pro-rata to all token holders")
    public DividendSummaryResponse distribute(
            @PathVariable UUID contractId,
            @Valid @RequestBody DistributeDividendRequest request) {
        return service.distribute(contractId, request);
    }
}
