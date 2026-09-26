package com.tokenrealty.reporting.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "valuation_approved_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValuationApprovedRecord extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "valuation_request_id", nullable = false)
    private UUID valuationRequestId;

    @Column(name = "value_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal valueUsd;

    @Column(name = "nav_per_token_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal navPerTokenUsd;

    @Column(name = "total_tokens", nullable = false)
    private long totalTokens;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;
}
