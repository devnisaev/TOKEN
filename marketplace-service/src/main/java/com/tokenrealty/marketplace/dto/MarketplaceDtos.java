package com.tokenrealty.marketplace.dto;

import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class MarketplaceDtos {

    private MarketplaceDtos() {
    }

    @Builder
    public record CreateListingRequest(
            @NotNull UUID flatId,
            UUID contractId,
            @NotNull Listing.ListingType listingType,
            @NotNull @DecimalMin("0.01") BigDecimal priceUsd,
            @NotNull @Min(1) Long tokensTotal,
            @NotNull @Min(1) Long minInvestmentTokens,
            @Size(max = 500) String title,
            @Size(max = 2000) String description,
            UUID sellerInvestorId,
            String sellerWallet
    ) {
    }

    @Builder
    public record ListingResponse(
            UUID id,
            UUID flatId,
            UUID contractId,
            Listing.ListingType listingType,
            Listing.ListingStatus status,
            BigDecimal priceUsd,
            long tokensAvailable,
            long tokensTotal,
            long minInvestmentTokens,
            String title,
            String description,
            UUID sellerInvestorId,
            String sellerWallet,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    @Builder
    public record CreateSecondaryListingRequest(
            @NotNull UUID flatId,
            @NotNull UUID contractId,
            @NotNull UUID sellerInvestorId,
            @NotBlank @Size(max = 66) String sellerWallet,
            @NotNull @DecimalMin("0.01") BigDecimal priceUsd,
            @NotNull @Min(1) Long tokenAmount,
            @Size(max = 500) String title,
            @Size(max = 2000) String description
    ) {
    }

    @Builder
    public record PlaceSellOrderRequest(
            @NotNull UUID flatId,
            @NotNull UUID contractId,
            @NotNull UUID sellerInvestorId,
            @NotBlank @Size(max = 66) String sellerWallet,
            @NotNull @DecimalMin("0.01") BigDecimal priceUsd,
            @NotNull @Min(1) Long tokenAmount,
            @Size(max = 500) String title,
            @Size(max = 2000) String description
    ) {
    }

    @Builder
    public record PlaceOrderRequest(
            @NotNull UUID listingId,
            @NotNull UUID buyerId,
            @NotBlank @Size(max = 66) String buyerWallet,
            @NotNull @Min(1) Long tokenAmount
    ) {
    }

    @Builder
    public record OrderResponse(
            UUID id,
            UUID listingId,
            UUID flatId,
            UUID contractId,
            MarketOrder.OrderType orderType,
            MarketOrder.OrderStatus status,
            UUID buyerId,
            UUID sellerId,
            String buyerWallet,
            String sellerWallet,
            Listing.ListingType listingType,
            long tokenAmount,
            BigDecimal totalPriceUsd,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    @Builder
    public record TradeResponse(
            UUID id,
            UUID orderId,
            UUID listingId,
            UUID flatId,
            UUID contractId,
            UUID buyerId,
            UUID sellerId,
            String buyerWallet,
            String sellerWallet,
            Listing.ListingType listingType,
            long tokenAmount,
            BigDecimal totalPriceUsd,
            Trade.TradeStatus status,
            UUID paymentId,
            UUID transferId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    @Builder
    public record SettleTradeRequest(
            UUID paymentId,
            UUID transferId
    ) {
    }
}
