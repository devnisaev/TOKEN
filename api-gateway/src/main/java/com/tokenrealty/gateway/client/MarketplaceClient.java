package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class MarketplaceClient extends DownstreamRestClientSupport {

    public MarketplaceClient(@Qualifier("marketplaceRestClient") RestClient restClient) {
        super(restClient);
    }

    public ListingView getListing(UUID listingId) {
        return get(
                "/v1/listings/{id}",
                ListingView.class,
                DownstreamServices.MARKETPLACE,
                listingId);
    }

    public OrderView getOrder(UUID orderId) {
        return get(
                "/v1/orders/{id}",
                OrderView.class,
                DownstreamServices.MARKETPLACE,
                orderId);
    }

    public TradeView getTrade(UUID orderId) {
        return getAllowNotFound(
                "/v1/orders/{id}/trade",
                TradeView.class,
                DownstreamServices.MARKETPLACE,
                orderId);
    }

    public ListingView findActiveListingByFlatId(UUID flatId) {
        SpringPage<ListingView> page = get(
                uriBuilder -> uriBuilder
                        .path("/v1/listings")
                        .queryParam("flatId", flatId)
                        .queryParam("status", "ACTIVE")
                        .queryParam("size", 1)
                        .build(),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.MARKETPLACE);
        if (page == null || page.content().isEmpty()) {
            return null;
        }
        return page.content().getFirst();
    }

    public record SpringPage<T>(List<T> content) {
    }

    public record ListingView(
            UUID id,
            UUID flatId,
            UUID contractId,
            String listingType,
            String status,
            BigDecimal priceUsd,
            long tokensAvailable,
            long tokensTotal,
            long minInvestmentTokens,
            String title
    ) {
    }

    public record OrderView(UUID id, String status) {
    }

    public record TradeView(UUID id, String status) {
    }
}
