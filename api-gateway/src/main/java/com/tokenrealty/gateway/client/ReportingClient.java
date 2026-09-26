package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

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
}
