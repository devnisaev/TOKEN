package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SearchClient extends DownstreamRestClientSupport {

    public SearchClient(@Qualifier("searchRestClient") RestClient restClient) {
        super(restClient);
    }

    public SpringPage<ListingSearchResult> searchListings(
            String q,
            String listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return get(
                uriBuilder -> buildListingsUri(uriBuilder, q, listingType, minPrice, maxPrice, pageable),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.SEARCH);
    }

    public SpringPage<BuildingSearchResult> searchBuildings(String q, Pageable pageable) {
        return get(
                uriBuilder -> buildBuildingsUri(uriBuilder, q, pageable),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.SEARCH);
    }

    private static URI buildListingsUri(
            UriBuilder uriBuilder,
            String q,
            String listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        var builder = uriBuilder.path("/v1/search/listings");
        if (q != null) {
            builder.queryParam("q", q);
        }
        if (listingType != null) {
            builder.queryParam("listingType", listingType);
        }
        if (minPrice != null) {
            builder.queryParam("minPrice", minPrice);
        }
        if (maxPrice != null) {
            builder.queryParam("maxPrice", maxPrice);
        }
        applyPageable(builder, pageable);
        return builder.build();
    }

    private static URI buildBuildingsUri(UriBuilder uriBuilder, String q, Pageable pageable) {
        var builder = uriBuilder.path("/v1/search/buildings");
        if (q != null) {
            builder.queryParam("q", q);
        }
        applyPageable(builder, pageable);
        return builder.build();
    }

    private static void applyPageable(UriBuilder builder, Pageable pageable) {
        builder.queryParam("page", pageable.getPageNumber());
        builder.queryParam("size", pageable.getPageSize());
        pageable.getSort().forEach(order ->
                builder.queryParam("sort", order.getProperty() + "," + order.getDirection().name().toLowerCase()));
    }

    public record SpringPage<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int size,
            int number
    ) {
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
    }

    public record BuildingSearchResult(
            UUID buildingId,
            Instant approvedAt,
            int flatCount,
            BigDecimal latestTokenPriceUsd,
            BigDecimal latestNavPerTokenUsd,
            Instant indexedAt
    ) {
    }
}
