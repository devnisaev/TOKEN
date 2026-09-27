package com.tokenrealty.payment.entity;

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
import java.util.UUID;

@Entity
@Table(name = "collateral_positions", indexes = {
        @Index(name = "idx_collateral_investor", columnList = "investor_id"),
        @Index(name = "idx_collateral_contract", columnList = "contract_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollateralPosition extends BaseEntity {

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "token_amount", nullable = false)
    private long tokenAmount;

    @Column(name = "nav_per_token_usd", nullable = false, precision = 19, scale = 8)
    private BigDecimal navPerTokenUsd;

    @Column(name = "liquidity_tier", nullable = false, length = 10)
    @Builder.Default
    private String liquidityTier = "TIER_1";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CollateralStatus status = CollateralStatus.LOCKED;

    public enum CollateralStatus {
        LOCKED, RELEASED, LIQUIDATED
    }
}
