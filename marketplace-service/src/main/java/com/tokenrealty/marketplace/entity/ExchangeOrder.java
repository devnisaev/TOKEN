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
@Table(name = "exchange_orders", indexes = {
        @Index(name = "idx_exchange_orders_contract", columnList = "contract_id"),
        @Index(name = "idx_exchange_orders_investor", columnList = "investor_id"),
        @Index(name = "idx_exchange_orders_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeOrder extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Column(name = "limit_price_usd", nullable = false, precision = 19, scale = 8)
    private BigDecimal limitPriceUsd;

    @Column(name = "original_quantity", nullable = false)
    private long originalQuantity;

    @Column(name = "filled_quantity", nullable = false)
    @Builder.Default
    private long filledQuantity = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "wallet_address", nullable = false, length = 66)
    private String walletAddress;

    @Column(name = "liquidity_tier", length = 20)
    private String liquidityTier;

    public long remainingQuantity() {
        return originalQuantity - filledQuantity;
    }

    public enum OrderSide {
        BID,
        ASK
    }

    public enum OrderStatus {
        OPEN,
        PARTIALLY_FILLED,
        FILLED,
        CANCELLED
    }
}
