package com.tokenrealty.compliance.config;

import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.entity.ComplianceRecord.ComplianceStatus;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevComplianceDataInitializer implements ApplicationRunner {

    /** Matches Hardhat account #1 in docs/hardhat-demo.md */
    public static final UUID DEMO_INVESTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final String DEMO_INVESTOR_WALLET = "0x70997970c51812dc3a010c724d1afe6fc599aa84";

    private final ComplianceRecordRepository repository;

    @Value("${tokenrealty.compliance.seed-dev-records:true}")
    private boolean seedDevRecords;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedDevRecords) {
            return;
        }
        if (repository.existsByWalletAddress(DEMO_INVESTOR_WALLET)) {
            return;
        }

        Instant now = Instant.now();
        repository.save(ComplianceRecord.builder()
                .investorId(DEMO_INVESTOR_ID)
                .walletAddress(DEMO_INVESTOR_WALLET)
                .fullName("Demo Investor")
                .countryCode("KG")
                .kycProvider("dev-seed")
                .kycReferenceId("DEV-KYC-001")
                .kycVerifiedAt(now)
                .kycExpiresAt(now.plus(365, ChronoUnit.DAYS))
                .status(ComplianceStatus.APPROVED)
                .build());
        log.info("Seeded demo KYC for investor {} wallet {}", DEMO_INVESTOR_ID, DEMO_INVESTOR_WALLET);
    }
}
