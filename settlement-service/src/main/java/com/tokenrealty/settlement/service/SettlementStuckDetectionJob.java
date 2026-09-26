package com.tokenrealty.settlement.service;

import com.tokenrealty.settlement.config.SettlementProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementStuckDetectionJob {

    private final SettlementSagaService sagaService;
    private final SettlementProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${tokenrealty.settlement.stuck-check-ms:300000}")
    public void detectStuckSagas() {
        Instant threshold = clock.instant().minus(properties.stuckSlaMinutes(), ChronoUnit.MINUTES);
        int marked = sagaService.markStuckSagas(threshold);
        if (marked > 0) {
            log.warn("Marked {} settlement saga(s) as STUCK (sla={} minutes)", marked, properties.stuckSlaMinutes());
        }
    }
}
