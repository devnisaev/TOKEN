package com.tokenrealty.marketplace.mapper;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceMapper {

    public ListingResponse toListingResponse(Listing listing) {
        return ListingResponse.builder()
                .id(listing.getId())
                .flatId(listing.getFlatId())
                .contractId(listing.getContractId())
                .listingType(listing.getListingType())
                .status(listing.getStatus())
                .priceUsd(listing.getPriceUsd())
                .tokensAvailable(listing.getTokensAvailable())
                .tokensTotal(listing.getTokensTotal())
                .minInvestmentTokens(listing.getMinInvestmentTokens())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .sellerInvestorId(listing.getSellerInvestorId())
                .sellerWallet(listing.getSellerWallet())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }

    public OrderResponse toOrderResponse(MarketOrder order) {
        return OrderResponse.builder()
                .id(order.getId())
                .listingId(order.getListingId())
                .flatId(order.getFlatId())
                .contractId(order.getContractId())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .buyerWallet(order.getBuyerWallet())
                .sellerWallet(order.getSellerWallet())
                .listingType(order.getListingType())
                .tokenAmount(order.getTokenAmount())
                .totalPriceUsd(order.getTotalPriceUsd())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public TradeResponse toTradeResponse(Trade trade) {
        return toTradeResponse(trade, null);
    }

    public TradeResponse toTradeResponse(Trade trade, MarketOrder order) {
        return TradeResponse.builder()
                .id(trade.getId())
                .orderId(trade.getOrderId())
                .listingId(trade.getListingId())
                .flatId(trade.getFlatId())
                .contractId(trade.getContractId())
                .buyerId(trade.getBuyerId())
                .sellerId(trade.getSellerId())
                .buyerWallet(order != null ? order.getBuyerWallet() : null)
                .sellerWallet(trade.getSellerWallet())
                .listingType(trade.getListingType())
                .tokenAmount(trade.getTokenAmount())
                .totalPriceUsd(trade.getTotalPriceUsd())
                .status(trade.getStatus())
                .paymentId(trade.getPaymentId())
                .transferId(trade.getTransferId())
                .createdAt(trade.getCreatedAt())
                .updatedAt(trade.getUpdatedAt())
                .build();
    }
}
