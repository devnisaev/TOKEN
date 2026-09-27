package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.dto.ReportingDtos.RecordEsgSnapshotRequest;
import com.tokenrealty.reporting.service.AssetHealthRecomputeService;
import com.tokenrealty.reporting.service.EsgSnapshotService;
import com.tokenrealty.reporting.service.OperatorKpiSnapshotService;
import com.tokenrealty.reporting.service.OperatorKpiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Operator automation — Phase 20 integration")
class OperatorAutomationIntegrationTest {

    @Autowired OperatorKpiSnapshotService operatorKpiSnapshotService;
    @Autowired AssetHealthRecomputeService assetHealthRecomputeService;
    @Autowired OperatorKpiService operatorKpiService;
    @Autowired EsgSnapshotService esgSnapshotService;

    @Test
    @DisplayName("Record KPI snapshot and recompute health from ESG")
    void snapshotAndRecompute() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        esgSnapshotService.record(new RecordEsgSnapshotRequest(
                flatId, buildingId,
                new BigDecimal("25.0"), "A", "LOW", new BigDecimal("88.0")));

        var recompute = assetHealthRecomputeService.recomputeFromEsgSnapshots();
        assertThat(recompute.assetsRecomputed()).isEqualTo(1);

        var snapshot = operatorKpiSnapshotService.recordNow();
        assertThat(snapshot.trackedAssetCount()).isEqualTo(1);
        assertThat(operatorKpiSnapshotService.listRecent()).hasSize(1);

        var kpis = operatorKpiService.dashboard();
        assertThat(kpis.openMaintenanceTicketCount()).isGreaterThanOrEqualTo(0);
        assertThat(kpis.averageHealthScore()).isEqualByComparingTo("85.80");
    }
}
