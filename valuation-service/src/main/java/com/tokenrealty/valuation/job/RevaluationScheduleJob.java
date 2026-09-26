package com.tokenrealty.valuation.job;

import com.tokenrealty.valuation.service.RevaluationScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevaluationScheduleJob {

    private final RevaluationScheduleService revaluationScheduleService;

    @Scheduled(fixedDelayString = "${tokenrealty.valuation.revaluation.poll-ms:60000}")
    public void runDueSchedules() {
        int processed = revaluationScheduleService.processDueSchedules();
        if (processed > 0) {
            log.info("Processed {} due revaluation schedule(s)", processed);
        }
    }
}
