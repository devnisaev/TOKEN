package com.tokenrealty.marketplace.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class ValuationClient extends DownstreamRestClientSupport {

    public ValuationClient(@Qualifier("valuationRestClient") RestClient restClient) {
        super(restClient);
    }

    public NavSnapshotView getLatestNav(UUID flatId) {
        return getAllowNotFound(
                "/v1/valuations/flat/{flatId}/nav",
                NavSnapshotView.class,
                DownstreamServices.VALUATION,
                flatId);
    }

    public record NavSnapshotView(
            UUID id,
            UUID flatId,
            UUID buildingId,
            BigDecimal valueUsd,
            long totalTokens,
            BigDecimal navPerTokenUsd,
            Instant approvedAt
    ) {
    }
}
