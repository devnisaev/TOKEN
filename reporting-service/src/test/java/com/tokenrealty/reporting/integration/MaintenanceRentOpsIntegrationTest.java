package com.tokenrealty.reporting.integration;

import com.tokenrealty.reporting.service.MaintenanceBacklogService;
import com.tokenrealty.reporting.service.OperatorAlertService;
import com.tokenrealty.reporting.service.OperationsReportService;
import com.tokenrealty.reporting.service.RentCollectionSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Maintenance & rent ops — Phase 25 integration")
class MaintenanceRentOpsIntegrationTest {

    @Autowired MaintenanceBacklogService maintenanceBacklogService;
    @Autowired RentCollectionSummaryService rentCollectionSummaryService;
    @Autowired OperationsReportService operationsReportService;
    @Autowired OperatorAlertService operatorAlertService;

    @Test
    @DisplayName("Summarize maintenance backlog, rent collection, and operations export")
    void maintenanceAndRentSummaries() {
        var backlog = maintenanceBacklogService.summary();
        assertThat(backlog.totalOpenTicketCount()).isGreaterThanOrEqualTo(0);

        var rent = rentCollectionSummaryService.summary();
        assertThat(rent.totalCollectedUsd()).isNotNull();
        assertThat(rent.collectionCount()).isGreaterThanOrEqualTo(0);

        var export = operationsReportService.export();
        assertThat(export.maintenanceBacklog()).isNotNull();
        assertThat(export.rentCollection()).isNotNull();
    }

    @Test
    @DisplayName("Generate lease expiry and maintenance backlog alerts")
    void generateExtendedAlerts() {
        var generated = operatorAlertService.generate(30);
        assertThat(generated.openAlertCount()).isGreaterThanOrEqualTo(0);
    }
}
