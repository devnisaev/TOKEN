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
@Table(name = "esg_profiles", indexes = {
        @Index(name = "idx_esg_flat", columnList = "flat_id"),
        @Index(name = "idx_esg_building", columnList = "building_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsgProfile extends BaseEntity {

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "carbon_score", precision = 5, scale = 2)
    private BigDecimal carbonScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_rating", length = 5)
    private EnergyRating energyRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "environmental_risk_tier", length = 10)
    private EnvironmentalRiskTier environmentalRiskTier;

    @Column(name = "last_assessed_at")
    private Instant lastAssessedAt;

    public enum EnergyRating {
        A, B, C, D, E, F, G
    }

    public enum EnvironmentalRiskTier {
        LOW, MEDIUM, HIGH
    }
}
