package com.tokenrealty.payment.dto;

import com.tokenrealty.payment.entity.*;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    @Builder
    public record InitiatePaymentRequest(
            @NotNull UUID orderId,
            @NotNull UUID payerId,
            @NotBlank @Size(max = 66) String payerWallet,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull PaymentCurrency currency,
            Payment.PaymentType paymentType
    ) {
        public Payment.PaymentType paymentType() {
            return paymentType != null ? paymentType : Payment.PaymentType.TOKEN_PURCHASE;
        }
    }

    @Builder
    public record ConfirmPaymentRequest(
            @NotBlank @Size(max = 66) String txHash
    ) {
    }

    @Builder
    public record PaymentResponse(
            UUID id,
            UUID orderId,
            UUID payerId,
            String payerWallet,
            BigDecimal amount,
            PaymentCurrency currency,
            Payment.PaymentStatus status,
            Payment.PaymentType paymentType,
            String txHash,
            Instant confirmedAt,
            EscrowResponse escrow,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    @Builder
    public record EscrowResponse(
            UUID id,
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            PaymentCurrency currency,
            Escrow.EscrowStatus status,
            String escrowWalletAddress
    ) {
    }

    @Builder
    public record CreatePayoutRequest(
            @NotNull UUID recipientInvestorId,
            @NotBlank @Size(max = 66) String recipientWallet,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull PaymentCurrency currency,
            @NotNull Payout.PayoutPurpose purpose,
            UUID referenceId,
            UUID flatId,
            UUID tenantId,
            @Size(max = 20) String period
    ) {
    }

    @Builder
    public record PayoutResponse(
            UUID id,
            UUID recipientInvestorId,
            String recipientWallet,
            BigDecimal amount,
            PaymentCurrency currency,
            Payout.PayoutPurpose purpose,
            UUID referenceId,
            String period,
            Payout.PayoutStatus status,
            String txHash,
            Instant completedAt,
            Instant createdAt
    ) {
    }

    @Builder
    public record WalletBalanceResponse(
            UUID investorId,
            PaymentCurrency currency,
            BigDecimal availableBalance,
            BigDecimal heldBalance
    ) {
    }
}
