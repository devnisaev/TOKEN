package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class ValuationClient extends DownstreamRestClientSupport {

    public ValuationClient(@Qualifier("valuationRestClient") RestClient restClient) {
        super(restClient);
    }

    public List<ValuationRequestView> listByBuilding(UUID buildingId) {
        ValuationRequestView[] views = getAllowNotFound(
                "/v1/valuations/building/{buildingId}",
                ValuationRequestView[].class,
                DownstreamServices.VALUATION,
                buildingId);
        if (views == null) {
            return List.of();
        }
        return Arrays.stream(views)
                .sorted(Comparator.comparing(ValuationRequestView::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public record ValuationRequestView(
            UUID id,
            UUID buildingId,
            UUID flatId,
            BigDecimal valueUsd,
            long totalTokens,
            String status,
            UUID submittedBy,
            UUID reviewedBy,
            String notes,
            String rejectionReason,
            Instant reviewedAt,
            UUID navSnapshotId,
            Instant createdAt
    ) {
    }
}
