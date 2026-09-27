package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.dto.ReportingDtos.RecordAssetHealthScoreRequest;
import com.tokenrealty.reporting.dto.ReportingDtos.RecordEsgSnapshotRequest;
import com.tokenrealty.reporting.service.AssetHealthScoreService;
import com.tokenrealty.reporting.service.EsgSnapshotService;
import com.tokenrealty.reporting.service.OperatorKpiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Asset health & operator KPIs — Phase 18 integration")
class AssetHealthIntegrationTest {

    @Autowired AssetHealthScoreService assetHealthScoreService;
    @Autowired EsgSnapshotService esgSnapshotService;
    @Autowired OperatorKpiService operatorKpiService;

    @Test
    @DisplayName("Record health score and aggregate operator KPIs")
    void healthScoreAndKpis() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        esgSnapshotService.record(new RecordEsgSnapshotRequest(
                flatId, buildingId,
                new BigDecimal("30.0"), "A", "LOW", new BigDecimal("85.0")));

        var score = assetHealthScoreService.record(new RecordAssetHealthScoreRequest(
                flatId, buildingId,
                new BigDecimal("80.0"), new BigDecimal("85.0"), new BigDecimal("90.0")));
        assertThat(score.healthScore()).isEqualByComparingTo("84.25");

        var kpis = operatorKpiService.dashboard();
        assertThat(kpis.trackedAssetCount()).isEqualTo(1);
        assertThat(kpis.averageHealthScore()).isEqualByComparingTo("84.25");
        assertThat(kpis.averageOccupancyPct()).isEqualByComparingTo("85.0");
    }
}
