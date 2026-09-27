package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.dto.ReportingDtos.RecordAssetHealthScoreRequest;
import com.tokenrealty.reporting.entity.OperatorAlertRecord;
import com.tokenrealty.reporting.repository.OperatorAlertRecordRepository;
import com.tokenrealty.reporting.service.AssetHealthScoreService;
import com.tokenrealty.reporting.service.HealthTrendService;
import com.tokenrealty.reporting.service.OperatorAlertSummaryService;
import com.tokenrealty.reporting.service.OperationsReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Operator trends — Phase 23 integration")
class OperatorTrendIntegrationTest {

    @Autowired AssetHealthScoreService assetHealthScoreService;
    @Autowired HealthTrendService healthTrendService;
    @Autowired OperatorAlertSummaryService operatorAlertSummaryService;
    @Autowired OperationsReportService operationsReportService;
    @Autowired OperatorAlertRecordRepository operatorAlertRecordRepository;

    @Test
    @DisplayName("Detect declining health and summarize alerts")
    void trendsAndExport() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        assetHealthScoreService.record(new RecordAssetHealthScoreRequest(
                flatId, buildingId,
                new BigDecimal("80.0"), new BigDecimal("80.0"), new BigDecimal("80.0")));
        assetHealthScoreService.record(new RecordAssetHealthScoreRequest(
                flatId, buildingId,
                new BigDecimal("60.0"), new BigDecimal("60.0"), new BigDecimal("60.0")));

        operatorAlertRecordRepository.save(OperatorAlertRecord.builder()
                .dedupeKey("TEST:1")
                .alertType("HEALTH_AT_RISK")
                .severity(OperatorAlertRecord.AlertSeverity.CRITICAL)
                .status(OperatorAlertRecord.AlertStatus.OPEN)
                .buildingId(buildingId)
                .flatId(flatId)
                .message("Test alert")
                .detectedAt(Instant.now())
                .build());

        assertThat(healthTrendService.listDeclining()).hasSize(1);
        assertThat(operatorAlertSummaryService.summary().openAlertCount()).isEqualTo(1);
        assertThat(operatorAlertSummaryService.summary().criticalOpenCount()).isEqualTo(1);

        var export = operationsReportService.export();
        assertThat(export.decliningHealthCount()).isEqualTo(1);
        assertThat(export.alertSummary().byType()).isNotEmpty();
        assertThat(export.leaseCoverage()).isNotNull();
    }
}
