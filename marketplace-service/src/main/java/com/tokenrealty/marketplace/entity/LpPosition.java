package com.tokenrealty.marketplace.entity;

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
@Table(name = "lp_positions", indexes = {
        @Index(name = "idx_lp_pool_investor", columnList = "pool_id, investor_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LpPosition extends BaseEntity {

    @Column(name = "pool_id", nullable = false)
    private UUID poolId;

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "lp_shares", nullable = false, precision = 24, scale = 8)
    private BigDecimal lpShares;

    @Column(name = "deposited_at", nullable = false)
    private Instant depositedAt;
}
