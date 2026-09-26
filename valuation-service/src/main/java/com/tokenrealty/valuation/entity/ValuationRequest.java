package com.tokenrealty.valuation.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "valuation_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ValuationRequest extends BaseEntity {

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "value_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal valueUsd;

    @Column(name = "total_tokens", nullable = false)
    private long totalTokens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ValuationRequestStatus status = ValuationRequestStatus.PENDING;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(length = 1000)
    private String notes;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "nav_snapshot_id")
    private UUID navSnapshotId;
}
