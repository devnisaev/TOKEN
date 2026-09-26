package com.tokenrealty.issuance.controller;

import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tokens/{contractId}/holders")
@Tag(name = "Ownership", description = "Token holder registry")
public class OwnershipController {

    private final TokenHolderRepository holderRepository;
    private final TokenContractRepository contractRepository;

    public OwnershipController(TokenHolderRepository holderRepository,
                               TokenContractRepository contractRepository) {
        this.holderRepository = holderRepository;
        this.contractRepository = contractRepository;
    }

    @GetMapping
    @Operation(summary = "List all token holders for a contract")
    public Page<TokenHolderResponse> listHolders(
            @PathVariable UUID contractId,
            @PageableDefault(size = 20) Pageable pageable) {
        if (!contractRepository.existsById(contractId)) {
            throw new ResourceNotFoundException("TokenContract", contractId);
        }
        return holderRepository.findByTokenContractId(contractId, pageable)
                .map(this::toResponse);
    }

    @GetMapping("/{holderId}")
    @Operation(summary = "Get a specific holder")
    public TokenHolderResponse getHolder(
            @PathVariable UUID contractId,
            @PathVariable UUID holderId) {
        return holderRepository.findById(holderId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("TokenHolder", holderId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new token holder (e.g. after off-chain purchase)")
    public TokenHolderResponse registerHolder(
            @PathVariable UUID contractId,
            @RequestBody RegisterHolderRequest request) {
        var contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("TokenContract", contractId));

        BigDecimal ownershipPct = BigDecimal.valueOf(request.initialBalance())
                .divide(BigDecimal.valueOf(contract.getTotalSupply()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        TokenHolder holder = TokenHolder.builder()
                .tokenContract(contract)
                .investorId(request.investorId())
                .walletAddress(request.walletAddress())
                .balance(request.initialBalance())
                .balanceUsd(contract.getTokenPriceUsd()
                        .multiply(BigDecimal.valueOf(request.initialBalance())))
                .ownershipPercentage(ownershipPct)
                .kycVerified(false)
                .whitelistedOnChain(false)
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build();

        return toResponse(holderRepository.save(holder));
    }

    private TokenHolderResponse toResponse(TokenHolder h) {
        return new TokenHolderResponse(
                h.getId(),
                h.getTokenContract().getId(),
                h.getInvestorId(),
                h.getWalletAddress(),
                h.getBalance(),
                h.getBalanceUsd(),
                h.getOwnershipPercentage(),
                h.getKycVerified(),
                h.getWhitelistedOnChain(),
                h.getStatus(),
                h.getCreatedAt()
        );
    }
}
