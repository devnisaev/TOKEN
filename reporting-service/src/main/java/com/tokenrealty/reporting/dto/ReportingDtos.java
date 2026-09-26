package com.tokenrealty.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReportingDtos {

    private ReportingDtos() {
    }

    public record TradingSummaryResponse(
            long settledTradeCount,
            long matchedOrderCount,
            BigDecimal matchedVolumeUsd,
            Instant generatedAt
    ) {
    }

    public record OccupancyResponse(
            long tokenizedFlatCount,
            long occupiedFlatCount,
            BigDecimal occupancyRate,
            Instant generatedAt
    ) {
    }

    public record DividendSummaryItem(
            UUID id,
            UUID flatId,
            UUID contractId,
            String period,
            BigDecimal totalAmountUsd,
            int holderCount,
            Instant distributedAt
    ) {
    }

    public record DividendsResponse(
            List<DividendSummaryItem> dividends,
            BigDecimal totalDistributedUsd,
            Instant generatedAt
    ) {
    }

    public record RegulatoryExportItem(
            String recordType,
            UUID referenceId,
            Instant occurredAt,
            String details
    ) {
    }

    public record RegulatoryExportResponse(
            String format,
            List<RegulatoryExportItem> items,
            Instant generatedAt
    ) {
    }
}
