package com.tokenrealty.payment.controller;

import com.tokenrealty.payment.dto.LendingDtos.*;
import com.tokenrealty.payment.service.LendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/lending")
@RequiredArgsConstructor
@Tag(name = "Token Collateral Lending", description = "Borrow USDC against staked property tokens (Phase 15)")
public class LendingController {

    private final LendingService lendingService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Collateral and loan dashboard for an investor")
    public LendingDashboardResponse dashboard(@RequestParam UUID investorId) {
        return lendingService.getDashboard(investorId);
    }

    @PostMapping("/collateral")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Deposit token collateral")
    public CollateralPositionResponse deposit(@Valid @RequestBody DepositCollateralRequest request) {
        return lendingService.depositCollateral(request);
    }

    @PostMapping("/borrow")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Borrow USDC against locked collateral")
    public LoanAccountResponse borrow(@Valid @RequestBody BorrowAgainstCollateralRequest request) {
        return lendingService.borrow(request);
    }

    @PostMapping("/loans/{loanId}/repay")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Repay loan principal and interest")
    public LoanAccountResponse repay(
            @PathVariable UUID loanId,
            @Valid @RequestBody RepayLoanRequest request) {
        return lendingService.repay(loanId, request);
    }
}
