package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.dto.ReportingDtos.RecordAssetHealthScoreRequest;
import com.tokenrealty.reporting.service.AssetHealthScoreService;
import com.tokenrealty.reporting.service.BuildingHealthService;
import com.tokenrealty.reporting.service.OperatorAlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Portfolio health — Phase 21 integration")
class PortfolioHealthIntegrationTest {

    @Autowired BuildingHealthService buildingHealthService;
    @Autowired AssetHealthScoreService assetHealthScoreService;
    @Autowired OperatorAlertService operatorAlertService;

    @Test
    @DisplayName("Building rollup and portfolio health by flat IDs")
    void buildingAndPortfolioHealth() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        assetHealthScoreService.record(new RecordAssetHealthScoreRequest(
                flatId, buildingId,
                new BigDecimal("70.0"), new BigDecimal("80.0"), new BigDecimal("90.0")));

        var rollup = buildingHealthService.getBuildingRollup(buildingId);
        assertThat(rollup.assetCount()).isEqualTo(1);
        assertThat(rollup.averageHealthScore()).isEqualByComparingTo("78.50");

        var portfolio = buildingHealthService.portfolioHealth(List.of(flatId));
        assertThat(portfolio).hasSize(1);
        assertThat(portfolio.getFirst().healthScore()).isEqualByComparingTo("78.50");

        operatorAlertService.generate(30);
        assertThat(operatorAlertService.listAcknowledged(PageRequest.of(0, 10)).getTotalElements())
                .isZero();
    }
}
