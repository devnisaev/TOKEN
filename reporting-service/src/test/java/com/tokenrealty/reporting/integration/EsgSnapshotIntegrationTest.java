package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.dto.ReportingDtos.RecordEsgSnapshotRequest;
import com.tokenrealty.reporting.service.EsgSnapshotService;
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
@DisplayName("ESG snapshot projections — Phase 17 integration")
class EsgSnapshotIntegrationTest {

    @Autowired EsgSnapshotService esgSnapshotService;

    @Test
    @DisplayName("Record and list ESG snapshot")
    void recordSnapshot() {
        UUID buildingId = UUID.randomUUID();
        esgSnapshotService.record(new RecordEsgSnapshotRequest(
                UUID.randomUUID(), buildingId,
                new BigDecimal("35.0"), "B", "LOW", new BigDecimal("72.5")));

        assertThat(esgSnapshotService.listByBuilding(buildingId)).hasSize(1);
    }
}
