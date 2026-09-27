package com.tokenrealty.reporting.controller;

import com.tokenrealty.reporting.dto.ReportingDtos.*;
import com.tokenrealty.reporting.service.AssetHealthRecomputeService;
import com.tokenrealty.reporting.service.AssetHealthScoreService;
import com.tokenrealty.reporting.service.BuildingHealthService;
import com.tokenrealty.reporting.service.HealthTrendService;
import com.tokenrealty.reporting.service.OperatorAlertSummaryService;
import com.tokenrealty.reporting.service.LeaseCoverageService;
import com.tokenrealty.reporting.service.MaintenanceBacklogService;
import com.tokenrealty.reporting.service.OperationsReportService;
import com.tokenrealty.reporting.service.RentCollectionSummaryService;
import com.tokenrealty.reporting.service.EsgSnapshotService;
import com.tokenrealty.reporting.service.OperatorAlertService;
import com.tokenrealty.reporting.service.OperatorKpiService;
import com.tokenrealty.reporting.service.OperatorKpiSnapshotService;
import com.tokenrealty.reporting.service.ReportingQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingQueryService queryService;
    private final EsgSnapshotService esgSnapshotService;
    private final AssetHealthScoreService assetHealthScoreService;
    private final OperatorKpiService operatorKpiService;
    private final OperatorAlertService operatorAlertService;
    private final OperatorKpiSnapshotService operatorKpiSnapshotService;
    private final AssetHealthRecomputeService assetHealthRecomputeService;
    private final BuildingHealthService buildingHealthService;
    private final OperatorAlertSummaryService operatorAlertSummaryService;
    private final HealthTrendService healthTrendService;
    private final OperationsReportService operationsReportService;
    private final LeaseCoverageService leaseCoverageService;
    private final MaintenanceBacklogService maintenanceBacklogService;
    private final RentCollectionSummaryService rentCollectionSummaryService;

    @GetMapping("/trading-summary")
    public TradingSummaryResponse tradingSummary() {
        return queryService.tradingSummary();
    }

    @GetMapping("/occupancy")
    public OccupancyResponse occupancy() {
        return queryService.occupancy();
    }

    @GetMapping("/dividends")
    public DividendsResponse dividends() {
        return queryService.dividends();
    }

    @GetMapping("/export")
    public RegulatoryExportResponse export(
            @RequestParam(defaultValue = "json") String format) {
        return queryService.regulatoryExport(format);
    }

    @GetMapping("/tax-summaries")
    public Page<TaxSummaryItem> taxSummaries(
            @RequestParam(required = false) UUID recipientInvestorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return queryService.taxSummaries(recipientInvestorId, pageable);
    }

    @GetMapping("/surveillance-alerts")
    public Page<SurveillanceAlertItem> surveillanceAlerts(
            @PageableDefault(size = 20) Pageable pageable) {
        return queryService.surveillanceAlerts(pageable);
    }

    @GetMapping("/esg-snapshots")
    public List<EsgSnapshotItem> esgSnapshots(
            @RequestParam(required = false) UUID buildingId) {
        return buildingId != null
                ? esgSnapshotService.listByBuilding(buildingId)
                : esgSnapshotService.listAll();
    }

    @PostMapping("/esg-snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EsgSnapshotItem recordEsgSnapshot(@Valid @RequestBody RecordEsgSnapshotRequest request) {
        return esgSnapshotService.record(request);
    }

    @GetMapping("/operator-kpis")
    public OperatorKpiResponse operatorKpis() {
        return operatorKpiService.dashboard();
    }

    @GetMapping("/asset-health-scores")
    public List<AssetHealthScoreItem> assetHealthScores(
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) UUID flatId) {
        if (flatId != null) {
            return assetHealthScoreService.listHistoryByFlat(flatId);
        }
        return buildingId != null
                ? assetHealthScoreService.listByBuilding(buildingId)
                : assetHealthScoreService.listAll();
    }

    @PostMapping("/asset-health-scores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AssetHealthScoreItem recordAssetHealthScore(
            @Valid @RequestBody RecordAssetHealthScoreRequest request) {
        return assetHealthScoreService.record(request);
    }

    @GetMapping("/operator-alerts")
    public Page<OperatorAlertItem> operatorAlerts(@PageableDefault(size = 20) Pageable pageable) {
        return operatorAlertService.listOpen(pageable);
    }

    @GetMapping("/operator-alerts/acknowledged")
    public Page<OperatorAlertItem> acknowledgedOperatorAlerts(
            @PageableDefault(size = 20) Pageable pageable) {
        return operatorAlertService.listAcknowledged(pageable);
    }

    @PostMapping("/operator-alerts/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public GenerateOperatorAlertsResponse generateOperatorAlerts(
            @RequestParam(defaultValue = "30") int insuranceWithinDays) {
        return operatorAlertService.generate(insuranceWithinDays);
    }

    @PatchMapping("/operator-alerts/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    public OperatorAlertItem acknowledgeOperatorAlert(@PathVariable UUID id) {
        return operatorAlertService.acknowledge(id);
    }

    @GetMapping("/kpi-snapshots")
    public List<OperatorKpiSnapshotItem> kpiSnapshots() {
        return operatorKpiSnapshotService.listRecent();
    }

    @PostMapping("/kpi-snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public OperatorKpiSnapshotItem recordKpiSnapshot() {
        return operatorKpiSnapshotService.recordNow();
    }

    @PostMapping("/asset-health-scores/recompute")
    @PreAuthorize("hasRole('ADMIN')")
    public RecomputeAssetHealthResponse recomputeAssetHealthScores() {
        return assetHealthRecomputeService.recomputeFromEsgSnapshots();
    }

    @GetMapping("/building-health")
    public List<BuildingHealthItem> buildingHealthRollups() {
        return buildingHealthService.listBuildingRollups();
    }

    @GetMapping("/building-health/{buildingId}")
    public BuildingHealthItem buildingHealth(@PathVariable UUID buildingId) {
        return buildingHealthService.getBuildingRollup(buildingId);
    }

    @GetMapping("/portfolio-health")
    public List<PortfolioHealthItem> portfolioHealth(@RequestParam List<UUID> flatIds) {
        return buildingHealthService.portfolioHealth(flatIds);
    }

    @GetMapping("/operator-alert-summary")
    public OperatorAlertSummaryResponse operatorAlertSummary() {
        return operatorAlertSummaryService.summary();
    }

    @GetMapping("/health-trends/declining")
    public List<HealthTrendItem> decliningHealthTrends() {
        return healthTrendService.listDeclining();
    }

    @GetMapping("/operations-export")
    public OperationsExportResponse operationsExport() {
        return operationsReportService.export();
    }

    @GetMapping("/lease-coverage")
    public LeaseCoverageSummary leaseCoverage(
            @RequestParam(defaultValue = "30") int expiringWithinDays) {
        return leaseCoverageService.summary(expiringWithinDays);
    }

    @GetMapping("/maintenance-backlog")
    public MaintenanceBacklogSummary maintenanceBacklog() {
        return maintenanceBacklogService.summary();
    }

    @GetMapping("/rent-collection-summary")
    public RentCollectionSummary rentCollectionSummary() {
        return rentCollectionSummaryService.summary();
    }
}
