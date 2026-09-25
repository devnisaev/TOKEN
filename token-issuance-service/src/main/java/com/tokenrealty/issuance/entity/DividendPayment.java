package com.tokenrealty.issuance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dividend_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DividendPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_contract_id", nullable = false)
    private TokenContract tokenContract;

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "investor_wallet", nullable = false)
    private String investorWallet;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "tokens_held", nullable = false)
    private Long tokensHeld;

    @Column(name = "ownership_pct", precision = 8, scale = 4)
    private BigDecimal ownershipPct;

    @Column(name = "gross_rental_income_usd", precision = 18, scale = 2)
    private BigDecimal grossRentalIncomeUsd;

    @Column(name = "amount_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountUsd;     // investor's share

    @Column(name = "amount_matic", precision = 18, scale = 8)
    private BigDecimal amountMatic;   // paid in MATIC on Polygon

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        PAID,
        FAILED
    }
}
