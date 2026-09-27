package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.dto.ReportingDtos.RecordAssetHealthScoreRequest;
import com.tokenrealty.reporting.service.AssetHealthScoreService;
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
@DisplayName("Asset health history — Phase 22 integration")
class AssetHealthHistoryIntegrationTest {

    @Autowired AssetHealthScoreService assetHealthScoreService;

    @Test
    @DisplayName("List health score history for a flat")
    void flatHistory() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        assetHealthScoreService.record(new RecordAssetHealthScoreRequest(
                flatId, buildingId,
                new BigDecimal("60.0"), new BigDecimal("70.0"), new BigDecimal("80.0")));
        assetHealthScoreService.record(new RecordAssetHealthScoreRequest(
                flatId, buildingId,
                new BigDecimal("65.0"), new BigDecimal("75.0"), new BigDecimal("85.0")));

        assertThat(assetHealthScoreService.listHistoryByFlat(flatId)).hasSize(2);
        assertThat(assetHealthScoreService.listHistoryByFlat(UUID.randomUUID())).isEmpty();
    }
}
