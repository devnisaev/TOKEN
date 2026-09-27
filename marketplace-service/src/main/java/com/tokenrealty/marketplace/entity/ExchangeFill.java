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
@Table(name = "exchange_fills", indexes = {
        @Index(name = "idx_exchange_fills_contract", columnList = "contract_id"),
        @Index(name = "idx_exchange_fills_executed_at", columnList = "executed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeFill extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "bid_order_id", nullable = false)
    private UUID bidOrderId;

    @Column(name = "ask_order_id", nullable = false)
    private UUID askOrderId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "buyer_wallet", length = 66)
    private String buyerWallet;

    @Column(name = "seller_wallet", length = 66)
    private String sellerWallet;

    @Column(name = "price_per_token_usd", nullable = false, precision = 19, scale = 8)
    private BigDecimal pricePerTokenUsd;

    @Column(name = "token_amount", nullable = false)
    private long tokenAmount;

    @Column(name = "total_price_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPriceUsd;

    @Column(name = "settlement_order_id")
    private UUID settlementOrderId;

    @Column(name = "settlement_trade_id")
    private UUID settlementTradeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private FillStatus status = FillStatus.PENDING_SETTLEMENT;

    @Column(name = "executed_at", nullable = false)
    private java.time.Instant executedAt;

    public enum FillStatus {
        PENDING_SETTLEMENT,
        SETTLED
    }
}
