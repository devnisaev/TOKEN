package com.tokenrealty.marketplace.entity;

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
@Table(name = "liquidity_pools", indexes = {
        @Index(name = "idx_pools_contract", columnList = "contract_id", unique = true),
        @Index(name = "idx_pools_flat", columnList = "flat_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidityPool extends BaseEntity {

    @Column(name = "contract_id", nullable = false, unique = true)
    private UUID contractId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "token_reserve", nullable = false)
    @Builder.Default
    private long tokenReserve = 0L;

    @Column(name = "usdc_reserve", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal usdcReserve = BigDecimal.ZERO;

    @Column(name = "total_lp_shares", nullable = false, precision = 24, scale = 8)
    @Builder.Default
    private BigDecimal totalLpShares = BigDecimal.ZERO;

    @Column(name = "fee_bps", nullable = false)
    @Builder.Default
    private int feeBps = 30;

    @Column(name = "nav_break_pct", nullable = false, precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal navBreakPct = new BigDecimal("10");

    @Column(name = "liquidity_tier", length = 20)
    @Builder.Default
    private String liquidityTier = "TIER_1";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PoolStatus status = PoolStatus.ACTIVE;

    @Column(name = "last_nav_per_token_usd", precision = 19, scale = 8)
    private BigDecimal lastNavPerTokenUsd;

    @Column(name = "last_nav_checked_at")
    private Instant lastNavCheckedAt;

    @Column(name = "lp_lock_days")
    @Builder.Default
    private int lpLockDays = 0;

    public enum PoolStatus {
        ACTIVE,
        PAUSED,
        CLOSED
    }
}
