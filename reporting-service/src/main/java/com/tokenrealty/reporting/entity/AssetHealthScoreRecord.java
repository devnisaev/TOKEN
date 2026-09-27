package com.tokenrealty.reporting.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "asset_health_score_records", indexes = {
        @Index(name = "idx_asset_health_flat", columnList = "flat_id"),
        @Index(name = "idx_asset_health_building", columnList = "building_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetHealthScoreRecord extends BaseEntity {

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "health_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal healthScore;

    @Column(name = "esg_factor", precision = 5, scale = 2)
    private BigDecimal esgFactor;

    @Column(name = "occupancy_factor", precision = 5, scale = 2)
    private BigDecimal occupancyFactor;

    @Column(name = "insurance_factor", precision = 5, scale = 2)
    private BigDecimal insuranceFactor;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
