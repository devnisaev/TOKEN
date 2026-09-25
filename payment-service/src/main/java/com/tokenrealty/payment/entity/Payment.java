package com.tokenrealty.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id"),
        @Index(name = "idx_payments_payer_id", columnList = "payer_id"),
        @Index(name = "uk_payments_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "payer_id", nullable = false)
    private UUID payerId;

    @Column(name = "payer_wallet", nullable = false, length = 66)
    private String payerWallet;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public enum PaymentStatus {
        PENDING,
        CONFIRMED,
        RELEASED,
        REFUNDED,
        FAILED
    }

    public enum PaymentType {
        TOKEN_PURCHASE,
        RENT
    }
}
