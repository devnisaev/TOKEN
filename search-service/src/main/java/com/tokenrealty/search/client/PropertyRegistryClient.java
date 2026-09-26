package com.tokenrealty.search.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
public class PropertyRegistryClient extends DownstreamRestClientSupport {

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        super(restClient);
    }

    public Optional<BuildingView> findBuilding(UUID buildingId) {
        BuildingView response = getAllowNotFound(
                "/v1/buildings/{id}",
                BuildingView.class,
                DownstreamServices.PROPERTY_REGISTRY,
                buildingId);
        return Optional.ofNullable(response);
    }

    public record BuildingView(
            UUID id,
            String name,
            String city
    ) {
    }
}
