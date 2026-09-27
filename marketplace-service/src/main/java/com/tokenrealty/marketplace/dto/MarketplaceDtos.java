package com.tokenrealty.marketplace.dto;

import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import jakarta.validation.constraints.*;
import lombok.Builder;

import com.tokenrealty.marketplace.entity.ExchangeFill;
import com.tokenrealty.marketplace.entity.ExchangeOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
            Listing.InstrumentType instrumentType,
            Listing.ListingStatus status,
            BigDecimal priceUsd,
            long tokensAvailable,
            long tokensTotal,
            long minInvestmentTokens,
            String title,
            String description,
            UUID sellerInvestorId,
            String sellerWallet,
            String propertyCategory,
            String operatingModel,
            String liquidityTier,
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

    @Builder
    public record PlaceExchangeOrderRequest(
            @NotNull UUID contractId,
            @NotNull UUID flatId,
            @NotNull UUID buildingId,
            @NotNull ExchangeOrder.OrderSide side,
            @NotNull @DecimalMin("0.01") BigDecimal limitPriceUsd,
            @NotNull @Min(1) Long quantity,
            @NotNull UUID investorId,
            @NotBlank @Size(max = 66) String walletAddress,
            @Size(max = 20) String liquidityTier
    ) {
    }

    @Builder
    public record ExchangeOrderResponse(
            UUID id,
            UUID contractId,
            UUID flatId,
            UUID buildingId,
            ExchangeOrder.OrderSide side,
            BigDecimal limitPriceUsd,
            long originalQuantity,
            long filledQuantity,
            long remainingQuantity,
            ExchangeOrder.OrderStatus status,
            UUID investorId,
            String walletAddress,
            String liquidityTier,
            int fillsOnPlacement,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    @Builder
    public record BookLevel(
            BigDecimal priceUsd,
            long totalQuantity,
            int orderCount
    ) {
    }

    @Builder
    public record BookDepthResponse(
            UUID contractId,
            List<BookLevel> bids,
            List<BookLevel> asks,
            Instant asOf
    ) {
    }

    @Builder
    public record ExchangeTradeResponse(
            UUID fillId,
            UUID contractId,
            BigDecimal pricePerTokenUsd,
            long tokenAmount,
            BigDecimal totalPriceUsd,
            Instant executedAt
    ) {
        public static ExchangeTradeResponse from(ExchangeFill fill) {
            return ExchangeTradeResponse.builder()
                    .fillId(fill.getId())
                    .contractId(fill.getContractId())
                    .pricePerTokenUsd(fill.getPricePerTokenUsd())
                    .tokenAmount(fill.getTokenAmount())
                    .totalPriceUsd(fill.getTotalPriceUsd())
                    .executedAt(fill.getExecutedAt())
                    .build();
        }
    }

    @Builder
    public record ExchangeTickerResponse(
            UUID contractId,
            BigDecimal lastPriceUsd,
            BigDecimal navPerTokenUsd,
            BigDecimal navDeltaPct,
            long volume24hTokens,
            BigDecimal notional24hUsd,
            Instant asOf
    ) {
    }
}
