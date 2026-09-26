package com.tokenrealty.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trades_order_id", columnList = "order_id"),
        @Index(name = "idx_trades_listing_id", columnList = "listing_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "token_amount", nullable = false)
    private long tokenAmount;

    @Column(name = "total_price_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPriceUsd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "transfer_id")
    private UUID transferId;

    public enum TradeStatus {
        PENDING,
        PAID,
        SETTLED,
        FAILED
    }
}
