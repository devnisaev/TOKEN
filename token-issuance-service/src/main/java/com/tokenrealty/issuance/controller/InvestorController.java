package com.tokenrealty.issuance.controller;

import com.tokenrealty.issuance.dto.IssuanceDtos.TokenHolderResponse;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/investors")
@RequiredArgsConstructor
@Tag(name = "Investors", description = "Cross-contract investor holdings")
public class InvestorController {

    private final TokenHolderRepository holderRepository;

    @GetMapping("/{investorId}/holdings")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "List token holdings for an investor across all contracts")
    public List<InvestorHoldingResponse> holdings(@PathVariable UUID investorId) {
        return holderRepository.findByInvestorId(investorId).stream()
                .map(this::toResponse)
                .toList();
    }

    private InvestorHoldingResponse toResponse(TokenHolder holder) {
        return new InvestorHoldingResponse(
                holder.getId(),
                holder.getTokenContract().getId(),
                holder.getInvestorId(),
                holder.getWalletAddress(),
                holder.getBalance(),
                holder.getBalanceUsd(),
                holder.getOwnershipPercentage(),
                holder.getStatus().name(),
                holder.getCreatedAt(),
                holder.getTokenContract().getTokenSymbol());
    }

    public record InvestorHoldingResponse(
            UUID id,
            UUID contractId,
            UUID investorId,
            String walletAddress,
            long balance,
            java.math.BigDecimal balanceUsd,
            java.math.BigDecimal ownershipPercentage,
            String status,
            java.time.Instant createdAt,
            String tokenSymbol
    ) {
    }
}
