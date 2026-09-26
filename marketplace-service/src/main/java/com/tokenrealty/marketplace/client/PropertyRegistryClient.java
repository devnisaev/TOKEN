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

    public void markFlatFullySold(UUID flatId) {
        patchVoid(
                uriBuilder -> uriBuilder
                        .path("/v1/flats/{id}/status")
                        .queryParam("status", "FULLY_SOLD")
                        .build(flatId),
                DownstreamServices.PROPERTY_REGISTRY);
    }
}
