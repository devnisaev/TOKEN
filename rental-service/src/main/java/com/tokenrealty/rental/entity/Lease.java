package com.tokenrealty.rental.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "leases", indexes = {
        @Index(name = "idx_leases_flat_id", columnList = "flat_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Lease extends BaseEntity {

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_wallet", nullable = false, length = 66)
    private String tenantWallet;

    @Column(name = "spv_recipient_id", nullable = false)
    private UUID spvRecipientId;

    @Column(name = "spv_wallet", nullable = false, length = 66)
    private String spvWallet;

    @Column(name = "monthly_rent_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyRentUsd;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaseStatus status;

    public enum LeaseStatus {
        ACTIVE,
        EXPIRED,
        TERMINATED
    }
}
