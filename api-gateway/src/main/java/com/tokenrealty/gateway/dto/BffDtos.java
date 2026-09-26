package com.tokenrealty.gateway.dto;

import com.tokenrealty.gateway.client.PropertyRegistryClient;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

public final class BffDtos {

    private BffDtos() {
    }

    @Builder
    public record FlatDetailResponse(
            UUID flatId,
            UUID buildingId,
            String buildingName,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            String status,
            TokenContractSummary tokenContract,
            ListingSummary listing
    ) {
    }

    @Builder
    public record ListingDetailResponse(
            ListingSummary listing,
            UUID flatId,
            String buildingName,
            String flatNumber,
            String flatStatus,
            TokenContractSummary tokenContract
    ) {
    }

    @Builder
    public record TokenContractSummary(
            UUID contractId,
            String contractAddress,
            String tokenSymbol,
            Long totalSupply,
            BigDecimal tokenPriceUsd,
            String status
    ) {
    }

    @Builder
    public record BuildingBffDetailResponse(
            PropertyRegistryClient.BuildingDetailView building,
            int tokenizedFlatCount,
            int availableFlatCount
    ) {
    }

    @Builder
    public record ListingSummary(
            UUID listingId,
            String listingType,
            String status,
            BigDecimal priceUsd,
            long tokensAvailable,
            long tokensTotal,
            long minInvestmentTokens,
            String title
    ) {
    }
}
