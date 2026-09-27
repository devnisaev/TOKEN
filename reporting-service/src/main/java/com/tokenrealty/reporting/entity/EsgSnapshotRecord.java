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
@Table(name = "esg_snapshot_records", indexes = {
        @Index(name = "idx_esg_snapshot_flat", columnList = "flat_id"),
        @Index(name = "idx_esg_snapshot_building", columnList = "building_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsgSnapshotRecord extends BaseEntity {

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "carbon_score", precision = 5, scale = 2)
    private BigDecimal carbonScore;

    @Column(name = "energy_rating", length = 5)
    private String energyRating;

    @Column(name = "environmental_risk_tier", length = 10)
    private String environmentalRiskTier;

    @Column(name = "occupancy_pct", precision = 5, scale = 2)
    private BigDecimal occupancyPct;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;
}
