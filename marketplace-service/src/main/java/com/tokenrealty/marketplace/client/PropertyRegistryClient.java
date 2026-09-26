package com.tokenrealty.marketplace.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class PropertyRegistryClient {

    private final RestClient restClient;

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void markFlatFullySold(UUID flatId) {
        restClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/flats/{id}/status")
                        .queryParam("status", "FULLY_SOLD")
                        .build(flatId))
                .retrieve()
                .toBodilessEntity();
    }
}
