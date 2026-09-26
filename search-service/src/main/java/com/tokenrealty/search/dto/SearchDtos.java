package com.tokenrealty.search.dto;

import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class SearchDtos {

    private SearchDtos() {
    }

    public record ListingSearchResult(
            UUID listingId,
            UUID flatId,
            UUID buildingId,
            String listingType,
            BigDecimal priceUsd,
            Long tokensAvailable,
            BigDecimal navPerTokenUsd,
            Instant indexedAt
    ) {
        public static ListingSearchResult from(ListingIndex index) {
            return new ListingSearchResult(
                    index.getListingId(),
                    index.getFlatId(),
                    index.getBuildingId(),
                    index.getListingType(),
                    index.getPriceUsd(),
                    index.getTokensAvailable(),
                    index.getNavPerTokenUsd(),
                    index.getIndexedAt());
        }
    }

    public record BuildingSearchResult(
            UUID buildingId,
            Instant approvedAt,
            int flatCount,
            BigDecimal latestTokenPriceUsd,
            BigDecimal latestNavPerTokenUsd,
            Instant indexedAt
    ) {
        public static BuildingSearchResult from(BuildingIndex index) {
            return new BuildingSearchResult(
                    index.getBuildingId(),
                    index.getApprovedAt(),
                    index.getFlatCount(),
                    index.getLatestTokenPriceUsd(),
                    index.getLatestNavPerTokenUsd(),
                    index.getIndexedAt());
        }
    }
}
