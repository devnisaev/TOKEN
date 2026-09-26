package com.tokenrealty.issuance.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PropertyRegistryClient extends DownstreamRestClientSupport {

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        super(restClient);
    }

    public FlatResponse getFlatById(UUID id) {
        return get(
                "/v1/flats/{id}",
                FlatResponse.class,
                DownstreamServices.PROPERTY_REGISTRY,
                "Flat not found: " + id,
                id);
    }

    public SpvResponse getSpvByBuilding(UUID buildingId) {
        return get(
                "/v1/buildings/{buildingId}/spv",
                SpvResponse.class,
                DownstreamServices.PROPERTY_REGISTRY,
                "SPV not found for building: " + buildingId,
                buildingId);
    }

    public FlatResponse setTokenInfo(
            UUID id,
            String contractAddress,
            Long totalTokens,
            BigDecimal tokenPriceUsd
    ) {
        return patch(
                uriBuilder -> uriBuilder
                        .path("/v1/flats/{id}/token-info")
                        .queryParam("contractAddress", contractAddress)
                        .queryParam("totalTokens", totalTokens)
                        .queryParam("tokenPriceUsd", tokenPriceUsd)
                        .build(id),
                FlatResponse.class,
                DownstreamServices.PROPERTY_REGISTRY);
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
