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

    public OperationsExportResponse export() {
        var kpis = operatorKpiService.dashboard();
        var alertSummary = operatorAlertSummaryService.summary();
        var declining = healthTrendService.listDeclining();
        return new OperationsExportResponse(
                kpis,
                alertSummary,
                declining.size(),
                declining,
                kpis.generatedAt());
    }
}
