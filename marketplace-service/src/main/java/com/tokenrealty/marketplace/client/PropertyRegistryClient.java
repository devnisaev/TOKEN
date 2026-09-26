package com.tokenrealty.marketplace.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class PropertyRegistryClient extends DownstreamRestClientSupport {

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        super(restClient);
    }

    public FlatView getFlat(UUID flatId) {
        return get(
                "/v1/flats/{id}",
                FlatView.class,
                DownstreamServices.PROPERTY_REGISTRY,
                "Flat not found: " + flatId,
                flatId);
    }

    public SpvView getSpvByBuilding(UUID buildingId) {
        return get(
                "/v1/buildings/{buildingId}/spv",
                SpvView.class,
                DownstreamServices.PROPERTY_REGISTRY,
                "SPV not found for building: " + buildingId,
                buildingId);
    }

    public void markFlatFullySold(UUID flatId) {
        patchVoid(
                uriBuilder -> uriBuilder
                        .path("/v1/flats/{id}/status")
                        .queryParam("status", "FULLY_SOLD")
                        .build(flatId),
                DownstreamServices.PROPERTY_REGISTRY);
    }

    public record FlatView(
            UUID id,
            UUID buildingId,
            String buildingName,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            String status
    ) {
    }

    public record SpvView(
            UUID id,
            UUID buildingId,
            String legalName,
            String registrationNumber,
            String walletAddress,
            String ownershipType,
            Boolean kycVerified,
            String status
    ) {
    }
}
