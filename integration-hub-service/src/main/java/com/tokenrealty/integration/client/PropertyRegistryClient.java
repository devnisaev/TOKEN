package com.tokenrealty.integration.client;

import com.tokenrealty.web.rest.DownstreamClientErrors;
import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PropertyRegistryClient extends DownstreamRestClientSupport {

    private final RestClient restClient;

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        super(restClient);
        this.restClient = restClient;
    }

    public void updateFlatOccupancy(UUID flatId, BigDecimal occupancyPct) {
        var body = new UpdateOccupancyBody(occupancyPct);
        DownstreamClientErrors.run(
                () -> restClient.patch()
                        .uri("/v1/flats/{id}/occupancy", flatId)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity(),
                DownstreamServices.PROPERTY_REGISTRY);
    }

    private record UpdateOccupancyBody(BigDecimal occupancyOrUtilization) {
    }
}
