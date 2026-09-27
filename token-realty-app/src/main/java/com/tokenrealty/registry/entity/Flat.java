package com.tokenrealty.registry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "flats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"building_id", "flat_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    private Integer floor;

    @Column(name = "area_sqm")
    private Double areaSqm;

    @Column(name = "net_usable_area_sqm")
    private Double netUsableAreaSqm;

    @Column(name = "cadastral_reference", length = 100)
    private String cadastralReference;

    @Column(name = "num_rooms")
    private Integer numRooms;

    @Column(name = "num_bathrooms")
    private Integer numBathrooms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FlatStatus status = FlatStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_model", length = 40)
    @Builder.Default
    private OperatingModel operatingModel = OperatingModel.PURE_RENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "liquidity_tier", length = 20)
    @Builder.Default
    private LiquidityTier liquidityTier = LiquidityTier.TIER_1;

    @Enumerated(EnumType.STRING)
    @Column(name = "development_stage", length = 30)
    private DevelopmentStage developmentStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "environmental_risk_tier", length = 20)
    private EnvironmentalRiskTier environmentalRiskTier;

    @ElementCollection
    @CollectionTable(name = "flat_license_types", joinColumns = @JoinColumn(name = "flat_id"))
    @Column(name = "license_type", length = 80)
    @Builder.Default
    private Set<String> licenseTypes = new HashSet<>();

    @Column(name = "occupancy_or_utilization", precision = 8, scale = 4)
    private BigDecimal occupancyOrUtilization;

    // Tokenization info — populated once tokens are issued
    @Column(name = "token_contract_address")
    private String tokenContractAddress;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "token_price_usd", precision = 18, scale = 2)
    private BigDecimal tokenPriceUsd;

    @OneToMany(mappedBy = "flat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Valuation> valuations = new ArrayList<>();

    @OneToMany(mappedBy = "flat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropertyDocument> documents = new ArrayList<>();

    public enum FlatStatus {
        AVAILABLE,        // Ready to be tokenized
        TOKENIZED,        // Token contract deployed
        FULLY_SOLD,       // All tokens sold
        SUSPENDED         // Temporarily halted
    }

    public enum OperatingModel {
        PURE_RENT,
        OPERATOR_REVENUE_SHARE,
        MEMBERSHIP,
        DEVELOPMENT
    }

    public enum LiquidityTier {
        TIER_1,
        TIER_2,
        TIER_3
    }

    public enum DevelopmentStage {
        RAW,
        PERMITTED,
        UNDER_CONSTRUCTION,
        STABILIZED
    }

    public enum EnvironmentalRiskTier {
        LOW,
        MEDIUM,
        HIGH
    }
}
