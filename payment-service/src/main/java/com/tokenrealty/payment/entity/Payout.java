package com.tokenrealty.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "payouts", indexes = {
        @Index(name = "idx_payouts_recipient_id", columnList = "recipient_investor_id"),
        @Index(name = "idx_payouts_reference_id", columnList = "reference_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payout extends BaseEntity {

    @Column(name = "recipient_investor_id", nullable = false)
    private UUID recipientInvestorId;

    @Column(name = "recipient_wallet", nullable = false, length = 66)
    private String recipientWallet;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutPurpose purpose;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(length = 20)
    private String period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum PayoutPurpose {
        DIVIDEND,
        RENT
    }

    public enum PayoutStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
