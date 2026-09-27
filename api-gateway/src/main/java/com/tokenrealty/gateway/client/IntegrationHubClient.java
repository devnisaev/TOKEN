package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Component
public class IntegrationHubClient extends DownstreamRestClientSupport {

    public IntegrationHubClient(@Qualifier("integrationHubRestClient") RestClient restClient) {
        super(restClient);
    }

    public SearchClient.SpringPage<CredentialView> listCredentials(Pageable pageable) {
        return get(
                uriBuilder -> buildPageUri(uriBuilder.path("/v1/integrations/credentials"), pageable),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.INTEGRATION_HUB);
    }

    public SearchClient.SpringPage<DeliveryView> listDeliveries(Pageable pageable) {
        return get(
                uriBuilder -> buildPageUri(uriBuilder.path("/v1/integrations/deliveries"), pageable),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.INTEGRATION_HUB);
    }

    private static URI buildPageUri(UriBuilder uriBuilder, Pageable pageable) {
        var builder = uriBuilder;
        builder.queryParam("page", pageable.getPageNumber());
        builder.queryParam("size", pageable.getPageSize());
        pageable.getSort().forEach(order ->
                builder.queryParam("sort", order.getProperty() + "," + order.getDirection().name().toLowerCase()));
        return builder.build();
    }

    public record CredentialView(
            UUID id,
            String integrationType,
            String provider,
            int version,
            Instant rotatedAt,
            Instant createdAt
    ) {
    }

    public record DeliveryView(
            UUID id,
            String integrationType,
            String provider,
            String payload,
            String status,
            int attempts,
            Instant nextRetryAt,
            String lastError,
            Instant createdAt
    ) {
    }
}
