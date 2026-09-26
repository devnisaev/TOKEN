package com.tokenrealty.gateway.client;

import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class MarketplaceClient {

    private final RestClient restClient;

    public MarketplaceClient(@Qualifier("marketplaceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ListingView getListing(UUID listingId) {
        try {
            return restClient.get()
                    .uri("/v1/listings/{id}", listingId)
                    .retrieve()
                    .body(ListingView.class);
        } catch (RestClientResponseException ex) {
            throw unavailable(ex);
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Marketplace service unavailable");
        }
    }

    public ListingView findActiveListingByFlatId(UUID flatId) {
        try {
            var page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/listings")
                            .queryParam("flatId", flatId)
                            .queryParam("status", "ACTIVE")
                            .queryParam("size", 1)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<SpringPage<ListingView>>() {
                    });
            if (page == null || page.content().isEmpty()) {
                return null;
            }
            return page.content().getFirst();
        } catch (RestClientResponseException ex) {
            throw unavailable(ex);
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Marketplace service unavailable");
        }
    }

    private static ValidationException unavailable(RestClientResponseException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            return new ValidationException("Marketplace service unavailable");
        }
        return new ValidationException("Marketplace request failed: " + ex.getStatusText());
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
}
