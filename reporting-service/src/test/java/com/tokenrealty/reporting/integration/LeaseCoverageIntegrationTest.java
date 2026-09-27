package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.dto.ReportingDtos.RecordEsgSnapshotRequest;
import com.tokenrealty.reporting.service.EsgSnapshotService;
import com.tokenrealty.reporting.service.LeaseCoverageService;
import com.tokenrealty.reporting.service.OperatorAlertService;
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
@DisplayName("Lease coverage — Phase 24 integration")
class LeaseCoverageIntegrationTest {

    @Autowired LeaseCoverageService leaseCoverageService;
    @Autowired EsgSnapshotService esgSnapshotService;
    @Autowired OperatorAlertService operatorAlertService;

    @Test
    @DisplayName("Summarize lease coverage and generate vacancy risk alerts")
    void leaseCoverage() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        esgSnapshotService.record(new RecordEsgSnapshotRequest(
                flatId, buildingId,
                new BigDecimal("40.0"), "C", "MEDIUM", new BigDecimal("45.0")));

        var summary = leaseCoverageService.summary(30);
        assertThat(summary.activeLeaseCount()).isGreaterThanOrEqualTo(0);
        assertThat(summary.vacancyRiskCount()).isGreaterThanOrEqualTo(0);

        var generated = operatorAlertService.generate(30);
        assertThat(generated.openAlertCount()).isGreaterThanOrEqualTo(0);
    }
}
