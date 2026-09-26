package com.tokenrealty.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "escrows", indexes = {
        @Index(name = "uk_escrows_payment_id", columnList = "payment_id", unique = true),
        @Index(name = "idx_escrows_order_id", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Escrow extends BaseEntity {

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EscrowStatus status;

    @Column(name = "escrow_wallet_address", nullable = false, length = 66)
    private String escrowWalletAddress;

    public enum EscrowStatus {
        AWAITING_DEPOSIT,
        HELD,
        RELEASED,
        REFUNDED
    }
}
