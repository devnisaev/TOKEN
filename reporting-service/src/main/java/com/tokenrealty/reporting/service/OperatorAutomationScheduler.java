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
}
