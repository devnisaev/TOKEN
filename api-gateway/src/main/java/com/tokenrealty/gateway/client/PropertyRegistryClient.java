package com.tokenrealty.gateway.client;

import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class PropertyRegistryClient {

    private final RestClient restClient;

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public BuildingDetailView getBuilding(UUID buildingId) {
        try {
            return restClient.get()
                    .uri("/v1/buildings/{id}", buildingId)
                    .retrieve()
                    .body(BuildingDetailView.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Building not found: " + buildingId);
            }
            throw unavailable(ex);
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Property Registry service unavailable");
        }
    }

    public FlatView getFlat(UUID flatId) {
        try {
            return restClient.get()
                    .uri("/v1/flats/{id}", flatId)
                    .retrieve()
                    .body(FlatView.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Flat not found: " + flatId);
            }
            throw unavailable(ex);
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Property Registry service unavailable");
        }
    }

    private static ValidationException unavailable(RestClientResponseException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            return new ValidationException("Property Registry service unavailable");
        }
        return new ValidationException("Property Registry request failed: " + ex.getStatusText());
    }

    public record FlatSummaryView(
            UUID id,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            String status,
            BigDecimal tokenPriceUsd
    ) {
    }

    public record SpvSummaryView(
            UUID id,
            String legalName,
            String registrationNumber,
            Boolean kycVerified,
            String status,
            String walletAddress
    ) {
    }

    public record BuildingDetailView(
            UUID id,
            String name,
            String address,
            String city,
            String country,
            String postalCode,
            Integer totalFloors,
            Integer totalFlats,
            Integer constructionYear,
            Double totalAreaSqm,
            String status,
            String propertyCategory,
            String cadastralReference,
            int flatCount,
            List<FlatSummaryView> flats,
            SpvSummaryView spv
    ) {
    }

    public record FlatView(
            UUID id,
            UUID buildingId,
            String buildingName,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            String status,
            String tokenContractAddress,
            Long totalTokens,
            BigDecimal tokenPriceUsd
    ) {
    }
}
