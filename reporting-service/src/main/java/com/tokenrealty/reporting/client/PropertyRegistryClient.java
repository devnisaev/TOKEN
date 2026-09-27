package com.tokenrealty.reporting.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class PropertyRegistryClient extends DownstreamRestClientSupport {

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        super(restClient);
    }

    public List<InsuranceExpiryAlertView> listExpiringInsurance(int withinDays) {
        return get(
                uriBuilder -> uriBuilder
                        .path("/v1/insurance/expiring")
                        .queryParam("withinDays", withinDays)
                        .build(),
                new ParameterizedTypeReference<List<InsuranceExpiryAlertView>>() {},
                DownstreamServices.PROPERTY_REGISTRY);
    }

    public record InsuranceExpiryAlertView(
            UUID id,
            UUID flatId,
            UUID buildingId,
            String provider,
            String policyNumber,
            BigDecimal coverageUsd,
            Instant expiresAt,
            long daysUntilExpiry) {
    }
}
