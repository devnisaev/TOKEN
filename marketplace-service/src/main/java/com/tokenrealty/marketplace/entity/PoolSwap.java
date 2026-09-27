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
import java.util.UUID;

@Entity
@Table(name = "pool_swaps", indexes = {
        @Index(name = "idx_pool_swaps_pool", columnList = "pool_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoolSwap extends BaseEntity {

    @Column(name = "pool_id", nullable = false)
    private UUID poolId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "swap_direction", nullable = false, length = 20)
    private SwapDirection swapDirection;

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "wallet_address", nullable = false, length = 66)
    private String walletAddress;

    @Column(name = "amount_in", nullable = false, precision = 19, scale = 8)
    private BigDecimal amountIn;

    @Column(name = "amount_out", nullable = false, precision = 19, scale = 8)
    private BigDecimal amountOut;

    @Column(name = "fee_usd", precision = 19, scale = 2)
    private BigDecimal feeUsd;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SwapStatus status = SwapStatus.PENDING;

    public enum SwapDirection {
        USDC_TO_TOKEN,
        TOKEN_TO_USDC
    }

    public enum SwapStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
