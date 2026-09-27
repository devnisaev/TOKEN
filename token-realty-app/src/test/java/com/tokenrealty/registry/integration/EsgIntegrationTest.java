package com.tokenrealty.registry.integration;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.EsgProfile;
import com.tokenrealty.registry.service.EsgService;
import com.tokenrealty.registry.service.InsuranceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ESG & insurance — Phase 17 integration")
class EsgIntegrationTest {

    @Autowired EsgService esgService;
    @Autowired InsuranceService insuranceService;

    @Test
    @DisplayName("Upsert ESG profile and register insurance policy")
    void esgAndInsurance() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        EsgProfileResponse esg = esgService.upsert(UpsertEsgProfileRequest.builder()
                .flatId(flatId)
                .buildingId(buildingId)
                .carbonScore(new BigDecimal("42.5"))
                .energyRating(EsgProfile.EnergyRating.B)
                .environmentalRiskTier(EsgProfile.EnvironmentalRiskTier.LOW)
                .build());
        assertThat(esg.carbonScore()).isEqualByComparingTo("42.5");

        InsurancePolicyResponse policy = insuranceService.create(CreateInsurancePolicyRequest.builder()
                .flatId(flatId)
                .buildingId(buildingId)
                .provider("Lloyd's RWA Cover")
                .policyNumber("POL-2026-001")
                .coverageUsd(new BigDecimal("5000000.00"))
                .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
                .build());
        assertThat(policy.status()).isEqualTo(com.tokenrealty.registry.entity.InsurancePolicy.PolicyStatus.ACTIVE);
    }
}
