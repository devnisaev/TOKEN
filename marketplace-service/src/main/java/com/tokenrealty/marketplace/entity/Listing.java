package com.tokenrealty.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "listings", indexes = {
        @Index(name = "idx_listings_flat_id", columnList = "flat_id"),
        @Index(name = "idx_listings_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing extends BaseEntity {

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 20)
    private ListingType listingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status;

    @Column(name = "price_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "tokens_available", nullable = false)
    private long tokensAvailable;

    @Column(name = "tokens_total", nullable = false)
    private long tokensTotal;

    @Column(name = "min_investment_tokens", nullable = false)
    private long minInvestmentTokens;

    @Column(length = 500)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "seller_investor_id")
    private UUID sellerInvestorId;

    public enum ListingType {
        PRIMARY,
        SECONDARY
    }

    public enum ListingStatus {
        ACTIVE,
        SOLD,
        EXPIRED,
        CANCELLED
    }
}
