package com.tokenrealty.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "market_orders", indexes = {
        @Index(name = "idx_orders_listing_id", columnList = "listing_id"),
        @Index(name = "idx_orders_buyer_id", columnList = "buyer_id"),
        @Index(name = "idx_orders_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketOrder extends BaseEntity {

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "buyer_id")
    private UUID buyerId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "buyer_wallet", length = 66)
    private String buyerWallet;

    @Column(name = "seller_wallet", length = 66)
    private String sellerWallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 20)
    private Listing.ListingType listingType;

    @Column(name = "token_amount", nullable = false)
    private long tokenAmount;

    @Column(name = "total_price_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPriceUsd;

    public enum OrderType {
        BUY,
        SELL
    }

    public enum OrderStatus {
        PENDING,
        MATCHED,
        PAID,
        SETTLED,
        CANCELLED
    }
}
