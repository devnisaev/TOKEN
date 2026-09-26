package com.tokenrealty.rental.config;

import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevRentalDataInitializer implements ApplicationRunner {

    public static final UUID DEMO_TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID DEMO_FLAT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DEMO_SPV_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String DEMO_TENANT_WALLET = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC";
    private static final String DEMO_SPV_WALLET = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";

    private final LeaseRepository leaseRepository;

    @Value("${tokenrealty.rental.seed-demo-lease:true}")
    private boolean seedDemoLease;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedDemoLease) {
            return;
        }
        if (leaseRepository.existsByFlatIdAndStatus(DEMO_FLAT_ID, Lease.LeaseStatus.ACTIVE)) {
            return;
        }
        leaseRepository.save(Lease.builder()
                .flatId(DEMO_FLAT_ID)
                .tenantId(DEMO_TENANT_ID)
                .tenantWallet(DEMO_TENANT_WALLET)
                .spvRecipientId(DEMO_SPV_ID)
                .spvWallet(DEMO_SPV_WALLET)
                .monthlyRentUsd(new BigDecimal("650.00"))
                .startDate(LocalDate.now().withDayOfMonth(1))
                .endDate(LocalDate.now().plusYears(1))
                .status(Lease.LeaseStatus.ACTIVE)
                .build());
        log.info("Seeded demo lease for tenant {} flat {}", DEMO_TENANT_ID, DEMO_FLAT_ID);
    }
}
