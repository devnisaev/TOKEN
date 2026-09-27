package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.OperationsExportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationsReportService {

    private final OperatorKpiService operatorKpiService;
    private final OperatorAlertSummaryService operatorAlertSummaryService;
    private final HealthTrendService healthTrendService;
    private final LeaseCoverageService leaseCoverageService;
    private final MaintenanceBacklogService maintenanceBacklogService;
    private final RentCollectionSummaryService rentCollectionSummaryService;

    public OperationsExportResponse export() {
        var kpis = operatorKpiService.dashboard();
        var alertSummary = operatorAlertSummaryService.summary();
        var declining = healthTrendService.listDeclining();
        var leaseCoverage = leaseCoverageService.summary(30);
        var maintenanceBacklog = maintenanceBacklogService.summary();
        var rentCollection = rentCollectionSummaryService.summary();
        return new OperationsExportResponse(
                kpis,
                alertSummary,
                declining.size(),
                declining,
                leaseCoverage,
                maintenanceBacklog,
                rentCollection,
                kpis.generatedAt());
    }
}
