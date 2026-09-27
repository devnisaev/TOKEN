package com.tokenrealty.payment.dto;

import com.tokenrealty.payment.entity.CollateralPosition;
import com.tokenrealty.payment.entity.LoanAccount;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class LendingDtos {

    private LendingDtos() {
    }

    @Builder
    public record DepositCollateralRequest(
            @NotNull UUID investorId,
            @NotNull UUID contractId,
            @NotNull @Min(1) Long tokenAmount,
            @NotNull @DecimalMin("0.01") BigDecimal navPerTokenUsd,
            @Size(max = 20) String liquidityTier
    ) {
    }

    @Builder
    public record BorrowAgainstCollateralRequest(
            @NotNull UUID investorId,
            @NotNull UUID collateralPositionId,
            @NotNull @DecimalMin("0.01") BigDecimal borrowAmountUsd
    ) {
    }

    @Builder
    public record RepayLoanRequest(
            @NotNull UUID investorId,
            @NotNull @DecimalMin("0.01") BigDecimal principalUsd,
            @DecimalMin("0") BigDecimal interestUsd
    ) {
    }

    @Builder
    public record CollateralPositionResponse(
            UUID id,
            UUID investorId,
            UUID contractId,
            long tokenAmount,
            BigDecimal navPerTokenUsd,
            BigDecimal collateralValueUsd,
            String liquidityTier,
            CollateralPosition.CollateralStatus status,
            Instant createdAt
    ) {
    }

    @Builder
    public record LoanAccountResponse(
            UUID id,
            UUID investorId,
            UUID collateralPositionId,
            BigDecimal principalUsd,
            BigDecimal outstandingUsd,
            int interestRateBps,
            LoanAccount.LoanStatus status,
            Instant createdAt
    ) {
    }

    @Builder
    public record LendingDashboardResponse(
            UUID investorId,
            BigDecimal totalCollateralUsd,
            BigDecimal totalOutstandingUsd,
            BigDecimal availableBorrowUsd,
            java.util.List<CollateralPositionResponse> collateral,
            java.util.List<LoanAccountResponse> loans
    ) {
    }
}
