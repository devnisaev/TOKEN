package com.tokenrealty.registry.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insurance_policies", indexes = {
        @Index(name = "idx_insurance_flat", columnList = "flat_id"),
        @Index(name = "idx_insurance_building", columnList = "building_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePolicy extends BaseEntity {

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(nullable = false, length = 120)
    private String provider;

    @Column(name = "policy_number", nullable = false, length = 64)
    private String policyNumber;

    @Column(name = "coverage_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal coverageUsd;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.ACTIVE;

    public enum PolicyStatus {
        ACTIVE, EXPIRED, CANCELLED
    }
}
