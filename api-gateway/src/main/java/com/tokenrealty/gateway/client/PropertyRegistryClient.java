package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class PropertyRegistryClient extends DownstreamRestClientSupport {

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        super(restClient);
    }

    public BuildingDetailView getBuilding(UUID buildingId) {
        return get(
                "/v1/buildings/{id}",
                BuildingDetailView.class,
                DownstreamServices.PROPERTY_REGISTRY,
                "Building not found: " + buildingId,
                buildingId);
    }

    public FlatView getFlat(UUID flatId) {
        return get(
                "/v1/flats/{id}",
                FlatView.class,
                DownstreamServices.PROPERTY_REGISTRY,
                "Flat not found: " + flatId,
                flatId);
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
