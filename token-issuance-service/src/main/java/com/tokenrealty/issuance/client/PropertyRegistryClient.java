package com.tokenrealty.issuance.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PropertyRegistryClient {

    private final RestClient restClient;

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public FlatResponse getFlatById(UUID id) {
        return restClient.get()
                .uri("/v1/flats/{id}", id)
                .retrieve()
                .body(FlatResponse.class);
    }

    public SpvResponse getSpvByBuilding(UUID buildingId) {
        return restClient.get()
                .uri("/v1/buildings/{buildingId}/spv", buildingId)
                .retrieve()
                .body(SpvResponse.class);
    }

    public FlatResponse setTokenInfo(
            UUID id,
            String contractAddress,
            Long totalTokens,
            BigDecimal tokenPriceUsd
    ) {
        return restClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/flats/{id}/token-info")
                        .queryParam("contractAddress", contractAddress)
                        .queryParam("totalTokens", totalTokens)
                        .queryParam("tokenPriceUsd", tokenPriceUsd)
                        .build(id))
                .retrieve()
                .body(FlatResponse.class);
    }

    // ─── DTOs (unchanged) ───────────────────────────────────────────────

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
    ) {}

    public record SpvResponse(
            UUID id,
            UUID buildingId,
            String legalName,
            String registrationNumber,
            String walletAddress,
            Boolean kycVerified,
            String status
    ) {}
}



/*package com.tokenrealty.issuance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(
        name = "property-registry",
        url = "${services.property-registry.url}"
)
public interface PropertyRegistryClient {

    @GetMapping("/v1/flats/{id}")
    FlatResponse getFlatById(@PathVariable UUID id);

    @GetMapping("/v1/buildings/{buildingId}/spv")
    SpvResponse getSpvByBuilding(@PathVariable UUID buildingId);

    @PatchMapping("/v1/flats/{id}/token-info")
    FlatResponse setTokenInfo(
            @PathVariable UUID id,
            @RequestParam String contractAddress,
            @RequestParam Long totalTokens,
            @RequestParam BigDecimal tokenPriceUsd);

    // ─── Response records (mirrors Property Registry DTOs) ─────────────────

    record FlatResponse(
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
    ) {}

    record SpvResponse(
            UUID id,
            UUID buildingId,
            String legalName,
            String registrationNumber,
            String walletAddress,
            Boolean kycVerified,
            String status
    ) {}
}*/