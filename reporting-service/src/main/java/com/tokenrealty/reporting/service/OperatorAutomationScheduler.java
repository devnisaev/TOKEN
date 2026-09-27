package com.tokenrealty.reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OperatorAutomationScheduler {

    private final OperatorAlertService operatorAlertService;
    private final OperatorKpiSnapshotService operatorKpiSnapshotService;
    private final OperationsReportService operationsReportService;
    private final com.tokenrealty.reporting.client.NotificationClient notificationClient;

    @Scheduled(cron = "${tokenrealty.reporting.alert-generation-cron:0 0 6,18 * * *}")
    public void generateOperatorAlerts() {
        var result = operatorAlertService.generate(30);
        log.debug("Scheduled alert generation created {} alerts ({} open)", result.alertsCreated(), result.openAlertCount());
    }

    @Scheduled(cron = "${tokenrealty.reporting.kpi-snapshot-cron:0 30 1 * * *}")
    public void recordKpiSnapshot() {
        operatorKpiSnapshotService.recordNow();
        log.debug("Scheduled KPI snapshot recorded");
    }

    @Scheduled(cron = "${tokenrealty.reporting.operator-digest-cron:0 0 8 * * MON}")
    public void sendWeeklyOperatorDigest() {
        var report = operationsReportService.export();
        notificationClient.sendOperatorDigest(
                report.alertSummary().openAlertCount(),
                report.alertSummary().criticalOpenCount(),
                report.decliningHealthCount(),
                report.kpis().averageHealthScore().toPlainString());
        log.debug("Weekly operator digest sent");
    }
}
