package com.tokenrealty.reporting.controller;

import com.tokenrealty.reporting.dto.ReportingDtos.*;
import com.tokenrealty.reporting.service.AssetHealthScoreService;
import com.tokenrealty.reporting.service.EsgSnapshotService;
import com.tokenrealty.reporting.service.OperatorKpiService;
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
            @RequestParam(required = false) UUID buildingId) {
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
}
