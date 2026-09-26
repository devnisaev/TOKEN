package com.tokenrealty.valuation.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nav_snapshots")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NavSnapshot extends BaseEntity {

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "valuation_request_id", nullable = false)
    private UUID valuationRequestId;

    @Column(name = "value_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal valueUsd;

    @Column(name = "total_tokens", nullable = false)
    private long totalTokens;

    @Column(name = "nav_per_token_usd", nullable = false, precision = 19, scale = 8)
    private BigDecimal navPerTokenUsd;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;
}
