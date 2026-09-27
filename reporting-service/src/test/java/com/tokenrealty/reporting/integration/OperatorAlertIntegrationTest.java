package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.dto.ReportingDtos.RecordAssetHealthScoreRequest;
import com.tokenrealty.reporting.dto.ReportingDtos.RecordEsgSnapshotRequest;
import com.tokenrealty.reporting.service.AssetHealthScoreService;
import com.tokenrealty.reporting.service.EsgSnapshotService;
import com.tokenrealty.reporting.service.OperatorAlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Operator alerts — Phase 19 integration")
class OperatorAlertIntegrationTest {

    @Autowired OperatorAlertService operatorAlertService;
    @Autowired AssetHealthScoreService assetHealthScoreService;
    @Autowired EsgSnapshotService esgSnapshotService;

    @Test
    @DisplayName("Generate alerts from health and occupancy projections")
    void generateAlerts() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        esgSnapshotService.record(new RecordEsgSnapshotRequest(
                flatId, buildingId,
                new BigDecimal("40.0"), "C", "MEDIUM", new BigDecimal("45.0")));

        assetHealthScoreService.record(new RecordAssetHealthScoreRequest(
                flatId, buildingId,
                new BigDecimal("40.0"), new BigDecimal("45.0"), new BigDecimal("50.0")));

        var result = operatorAlertService.generate(30);
        assertThat(result.alertsCreated()).isEqualTo(2);
        assertThat(operatorAlertService.listOpen(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);

        UUID alertId = operatorAlertService.listOpen(PageRequest.of(0, 1)).getContent().getFirst().id();
        operatorAlertService.acknowledge(alertId);
        assertThat(operatorAlertService.countOpen()).isEqualTo(1);
    }
}
