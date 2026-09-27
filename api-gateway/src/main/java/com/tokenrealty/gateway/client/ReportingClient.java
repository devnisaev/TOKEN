package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Component
public class ReportingClient extends DownstreamRestClientSupport {

    public ReportingClient(@Qualifier("reportingRestClient") RestClient restClient) {
        super(restClient);
    }

    public TradingSummaryView tradingSummary() {
        return get("/v1/reports/trading-summary", TradingSummaryView.class, DownstreamServices.REPORTING);
    }

    public OccupancyView occupancy() {
        return get("/v1/reports/occupancy", OccupancyView.class, DownstreamServices.REPORTING);
    }

    public DividendsView dividends() {
        return get("/v1/reports/dividends", DividendsView.class, DownstreamServices.REPORTING);
    }

    public SearchClient.SpringPage<TaxSummaryView> taxSummaries(UUID recipientInvestorId, Pageable pageable) {
        return get(
                uriBuilder -> buildTaxSummariesUri(uriBuilder, recipientInvestorId, pageable),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.REPORTING);
    }

    public SearchClient.SpringPage<SurveillanceAlertView> surveillanceAlerts(Pageable pageable) {
        return get(
                uriBuilder -> buildPageUri(uriBuilder.path("/v1/reports/surveillance-alerts"), pageable),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.REPORTING);
    }

    private static URI buildTaxSummariesUri(UriBuilder uriBuilder, UUID recipientInvestorId, Pageable pageable) {
        var builder = uriBuilder.path("/v1/reports/tax-summaries");
        if (recipientInvestorId != null) {
            builder.queryParam("recipientInvestorId", recipientInvestorId);
        }
        return buildPageUri(builder, pageable);
    }

    private static URI buildPageUri(UriBuilder uriBuilder, Pageable pageable) {
        var builder = uriBuilder;
        builder.queryParam("page", pageable.getPageNumber());
        builder.queryParam("size", pageable.getPageSize());
        pageable.getSort().forEach(order ->
                builder.queryParam("sort", order.getProperty() + "," + order.getDirection().name().toLowerCase()));
        return builder.build();
    }

    public record TradingSummaryView(
            long settledTradeCount,
            long matchedOrderCount,
            BigDecimal matchedVolumeUsd,
            Instant generatedAt
    ) {
    }

    public record OccupancyView(
            long tokenizedFlatCount,
            long occupiedFlatCount,
            BigDecimal occupancyRate,
            Instant generatedAt
    ) {
    }

    public record DividendsView(
            java.util.List<DividendItemView> dividends,
            BigDecimal totalDistributedUsd,
            Instant generatedAt
    ) {
    }

    public record DividendItemView(
            java.util.UUID id,
            java.util.UUID flatId,
            java.util.UUID contractId,
            String period,
            BigDecimal totalAmountUsd,
            int holderCount,
            Instant distributedAt
    ) {
    }

    public record TaxSummaryView(
            UUID id,
            UUID payoutId,
            UUID recipientInvestorId,
            BigDecimal grossAmountUsd,
            BigDecimal withholdingAmountUsd,
            BigDecimal netAmountUsd,
            Instant completedAt
    ) {
    }

    public record SurveillanceAlertView(
            UUID id,
            UUID orderId,
            UUID buyerId,
            UUID sellerId,
            String alertType,
            Instant detectedAt
    ) {
    }
}
