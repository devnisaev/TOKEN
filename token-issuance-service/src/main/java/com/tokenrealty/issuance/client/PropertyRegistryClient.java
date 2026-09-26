package com.tokenrealty.issuance.client;

import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PropertyRegistryClient {

    private final RestClient restClient;

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public FlatResponse getFlatById(UUID id) {
        try {
            return restClient.get()
                    .uri("/v1/flats/{id}", id)
                    .retrieve()
                    .body(FlatResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Flat not found: " + id);
            }
            throw registryUnavailable(ex);
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Property Registry service unavailable");
        }
    }

    public SpvResponse getSpvByBuilding(UUID buildingId) {
        try {
            return restClient.get()
                    .uri("/v1/buildings/{buildingId}/spv", buildingId)
                    .retrieve()
                    .body(SpvResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("SPV not found for building: " + buildingId);
            }
            throw registryUnavailable(ex);
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Property Registry service unavailable");
        }
    }

    public FlatResponse setTokenInfo(
            UUID id,
            String contractAddress,
            Long totalTokens,
            BigDecimal tokenPriceUsd
    ) {
        try {
            return restClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/flats/{id}/token-info")
                            .queryParam("contractAddress", contractAddress)
                            .queryParam("totalTokens", totalTokens)
                            .queryParam("tokenPriceUsd", tokenPriceUsd)
                            .build(id))
                    .retrieve()
                    .body(FlatResponse.class);
        } catch (RestClientResponseException ex) {
            throw new ValidationException("Property Registry token-info callback failed: " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Property Registry service unavailable");
        }
    }

    private static ValidationException registryUnavailable(RestClientResponseException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            return new ValidationException("Property Registry service unavailable");
        }
        return new ValidationException("Property Registry request failed: " + ex.getStatusText());
    }

    public record FlatResponse(
            UUID id,
            UUID buildingId,
            String buildingName,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            Integer numRooms,
            Integer numBathrooms,
            String status,
            String tokenContractAddress,
            Long totalTokens,
            BigDecimal tokenPriceUsd
    ) {
    }

    public record SpvResponse(
            UUID id,
            UUID buildingId,
            String legalName,
            String registrationNumber,
            String walletAddress,
            Boolean kycVerified,
            String status
    ) {
    }
}
