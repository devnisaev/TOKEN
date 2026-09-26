package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.entity.ComplianceRecord.ComplianceStatus;
import com.tokenrealty.compliance.kafka.port.KycEventPublisher;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycExpiryService {

    private final ComplianceRecordRepository repository;
    private final KycEventPublisher kycEventPublisher;

    @Scheduled(cron = "${tokenrealty.compliance.kyc-expiry.cron:0 0 6 * * *}")
    @Transactional
    public void expireOverdueKyc() {
        Instant now = Instant.now();
        var overdue = repository.findByStatusAndKycExpiresAtBefore(ComplianceStatus.APPROVED, now);
        if (overdue.isEmpty()) {
            return;
        }
        overdue.forEach(record -> expireRecord(record, now));
        log.info("Expired {} overdue KYC records", overdue.size());
    }

    private void expireRecord(ComplianceRecord record, Instant now) {
        record.setStatus(ComplianceStatus.EXPIRED);
        record.setRejectionReason("KYC expired");
        repository.save(record);
        kycEventPublisher.publishKycRevoked(
                record.getInvestorId(), record.getWalletAddress(), "expired", now);
    }
}
