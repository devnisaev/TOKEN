package com.tokenrealty.rental.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "rent_payments", indexes = {
        @Index(name = "idx_rent_payments_lease_period", columnList = "lease_id, period", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RentPayment extends BaseEntity {

    @Column(name = "lease_id", nullable = false)
    private UUID leaseId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "payout_id")
    private UUID payoutId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentPaymentStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    public enum RentPaymentStatus {
        PAID
    }
}
