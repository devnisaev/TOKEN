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

    public record TaxSummaryItem(
            UUID id,
            UUID payoutId,
            UUID recipientInvestorId,
            BigDecimal grossAmountUsd,
            BigDecimal withholdingAmountUsd,
            BigDecimal netAmountUsd,
            Instant completedAt
    ) {
    }

    public record SurveillanceAlertItem(
            UUID id,
            UUID orderId,
            UUID buyerId,
            UUID sellerId,
            String alertType,
            Instant detectedAt
    ) {
    }

    public record RecordEsgSnapshotRequest(
            UUID flatId,
            UUID buildingId,
            BigDecimal carbonScore,
            String energyRating,
            String environmentalRiskTier,
            BigDecimal occupancyPct
    ) {
    }

    public record EsgSnapshotItem(
            UUID id,
            UUID flatId,
            UUID buildingId,
            BigDecimal carbonScore,
            String energyRating,
            String environmentalRiskTier,
            BigDecimal occupancyPct,
            Instant snapshotAt
    ) {
    }

    public record RecordAssetHealthScoreRequest(
            UUID flatId,
            UUID buildingId,
            BigDecimal esgFactor,
            BigDecimal occupancyFactor,
            BigDecimal insuranceFactor
    ) {
    }

    public record AssetHealthScoreItem(
            UUID id,
            UUID flatId,
            UUID buildingId,
            BigDecimal healthScore,
            BigDecimal esgFactor,
            BigDecimal occupancyFactor,
            BigDecimal insuranceFactor,
            Instant computedAt
    ) {
    }

    public record OperatorKpiResponse(
            BigDecimal averageOccupancyPct,
            BigDecimal averageCarbonScore,
            BigDecimal averageHealthScore,
            long trackedAssetCount,
            long atRiskAssetCount,
            long esgSnapshotCount,
            long openOperatorAlertCount,
            long openMaintenanceTicketCount,
            Instant generatedAt
    ) {
    }

    public record OperatorKpiSnapshotItem(
            UUID id,
            BigDecimal averageOccupancyPct,
            BigDecimal averageCarbonScore,
            BigDecimal averageHealthScore,
            long trackedAssetCount,
            long atRiskAssetCount,
            long openOperatorAlertCount,
            long openMaintenanceTicketCount,
            Instant snapshotAt
    ) {
    }

    public record RecomputeAssetHealthResponse(int assetsRecomputed) {
    }

    public record OperatorAlertItem(
            UUID id,
            String alertType,
            String severity,
            String status,
            UUID referenceId,
            UUID buildingId,
            UUID flatId,
            String message,
            Instant detectedAt
    ) {
    }

    public record GenerateOperatorAlertsResponse(
            int alertsCreated,
            long openAlertCount
    ) {
    }

    public record BuildingHealthItem(
            UUID buildingId,
            BigDecimal averageHealthScore,
            long assetCount,
            long atRiskAssetCount
    ) {
    }

    public record PortfolioHealthItem(
            UUID flatId,
            UUID buildingId,
            BigDecimal healthScore,
            BigDecimal esgFactor,
            BigDecimal occupancyFactor
    ) {
    }

    public record AlertTypeCount(String alertType, long count) {
    }

    public record OperatorAlertSummaryResponse(
            long openAlertCount,
            long criticalOpenCount,
            List<AlertTypeCount> byType,
            Instant generatedAt
    ) {
    }

    public record HealthTrendItem(
            UUID flatId,
            UUID buildingId,
            BigDecimal previousScore,
            BigDecimal latestScore,
            BigDecimal delta,
            Instant latestAt
    ) {
    }

    public record OperationsExportResponse(
            OperatorKpiResponse kpis,
            OperatorAlertSummaryResponse alertSummary,
            long decliningHealthCount,
            List<HealthTrendItem> decliningHealth,
            Instant generatedAt
    ) {
    }
}
