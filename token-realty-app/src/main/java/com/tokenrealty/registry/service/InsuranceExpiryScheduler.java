package com.tokenrealty.registry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InsuranceExpiryScheduler {

    private final InsuranceService insuranceService;

    @Scheduled(cron = "${tokenrealty.registry.insurance.expiry-sweep-cron:0 0 2 * * *}")
    public void sweepExpiredPolicies() {
        insuranceService.expireOverduePolicies();
        log.debug("Insurance expiry sweep completed");
    }
}
