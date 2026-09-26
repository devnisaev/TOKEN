package com.tokenrealty.payment.controller;

import com.tokenrealty.payment.dto.PaymentDtos.WalletBalanceResponse;
import com.tokenrealty.payment.service.WalletBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/wallet-balances")
@RequiredArgsConstructor
@Tag(name = "Wallet Balances", description = "Off-chain fiat/stablecoin balances for investors")
public class WalletBalanceController {

    private final WalletBalanceService walletBalanceService;

    @GetMapping("/{investorId}")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Get USDC wallet balance for an investor")
    public WalletBalanceResponse getByInvestorId(@PathVariable UUID investorId) {
        return walletBalanceService.getByInvestorId(investorId);
    }
}
