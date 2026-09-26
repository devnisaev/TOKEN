package com.tokenrealty.issuance.dto;

import com.tokenrealty.issuance.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class IssuanceDtos {

    // ─── Token Contract ──────────────────────────────────────────────────────

    public record IssueTokenRequest(
            @NotNull UUID flatId,
            @NotNull UUID buildingId,
            @NotBlank String tokenName,
            @NotBlank @Size(max = 20) String tokenSymbol,
            @NotNull @Min(1) Long totalSupply,
            @NotNull @Positive BigDecimal tokenPriceUsd,
            @NotBlank String spvWalletAddress
    ) {}

    public record TokenContractResponse(
            UUID id,
            UUID flatId,
            UUID buildingId,
            String tokenName,
            String tokenSymbol,
            Long totalSupply,
            BigDecimal tokenPriceUsd,
            String contractAddress,
            String deploymentTxHash,
            Instant deployedAt,
            String network,
            Long chainId,
            TokenContract.ContractStatus status,
            String spvWalletAddress,
            int holderCount,
            Instant createdAt,
            Instant updatedAt
    ) {}

    // ─── Token Holder ────────────────────────────────────────────────────────

    public record RegisterHolderRequest(
            @NotNull UUID investorId,
            @NotBlank String walletAddress,
            @NotNull @Min(1) Long initialBalance
    ) {}

    public record TokenHolderResponse(
            UUID id,
            UUID contractId,
            UUID investorId,
            String walletAddress,
            Long balance,
            BigDecimal balanceUsd,
            BigDecimal ownershipPercentage,
            Boolean kycVerified,
            Boolean whitelistedOnChain,
            TokenHolder.HolderStatus status,
            Instant createdAt
    ) {}

    // ─── Transfer ────────────────────────────────────────────────────────────

    public record TransferRequest(
            @NotBlank String fromAddress,
            @NotBlank String toAddress,
            @NotNull @Min(1) Long amount,
            BigDecimal pricePerTokenUsd
    ) {}

    public record TokenTransferResponse(
            UUID id,
            UUID contractId,
            String fromAddress,
            String toAddress,
            Long amount,
            BigDecimal pricePerTokenUsd,
            BigDecimal totalValueUsd,
            String txHash,
            Long blockNumber,
            Instant confirmedAt,
            TokenTransfer.TransferType type,
            TokenTransfer.TransferStatus status,
            String failureReason,
            Instant createdAt
    ) {}

    // ─── Dividend ────────────────────────────────────────────────────────────

    public record DistributeDividendRequest(
            @NotNull LocalDate periodStart,
            @NotNull LocalDate periodEnd,
            @NotNull @Positive BigDecimal grossRentalIncomeUsd
    ) {}

    public record DividendPaymentResponse(
            UUID id,
            UUID contractId,
            UUID investorId,
            String investorWallet,
            LocalDate periodStart,
            LocalDate periodEnd,
            Long tokensHeld,
            BigDecimal ownershipPct,
            BigDecimal grossRentalIncomeUsd,
            BigDecimal amountUsd,
            BigDecimal amountMatic,
            String txHash,
            Instant paidAt,
            DividendPayment.PaymentStatus status,
            Instant createdAt
    ) {}

    public record DividendSummaryResponse(
            UUID contractId,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal grossRentalIncomeUsd,
            int recipientCount,
            BigDecimal totalDistributedUsd,
            String status
    ) {}

}
