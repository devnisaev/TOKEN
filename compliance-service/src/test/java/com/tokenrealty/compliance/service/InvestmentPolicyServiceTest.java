package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.entity.InvestmentPolicy;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import com.tokenrealty.compliance.repository.InvestmentPolicyRepository;
import com.tokenrealty.web.exception.ComplianceBlockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentPolicyService unit tests")
class InvestmentPolicyServiceTest {

    @Mock InvestmentPolicyRepository policyRepository;
    @Mock ComplianceRecordRepository complianceRecordRepository;
    @InjectMocks InvestmentPolicyService service;

    private UUID investorId;
    private InvestmentPolicy kgPolicy;

    @BeforeEach
    void setUp() {
        investorId = UUID.randomUUID();
        kgPolicy = InvestmentPolicy.builder()
                .jurisdiction("KG")
                .minInvestmentUsd(new BigDecimal("1000.00"))
                .maxInvestmentUsd(new BigDecimal("50000.00"))
                .accreditedOnly(true)
                .build();
    }

    @Test
    void checkInvestment_noPolicy_allows() {
        when(policyRepository.findByJurisdiction("US")).thenReturn(Optional.empty());

        var result = service.checkInvestment(investorId, "US", new BigDecimal("500.00"));

        assertThat(result.allowed()).isTrue();
        assertThat(result.minInvestmentUsd()).isNull();
    }

    @Test
    void checkInvestment_belowMinimum_blocks() {
        when(policyRepository.findByJurisdiction("KG")).thenReturn(Optional.of(kgPolicy));

        assertThatThrownBy(() -> service.checkInvestment(investorId, "KG", new BigDecimal("500.00")))
                .isInstanceOf(ComplianceBlockedException.class)
                .hasMessageContaining("below minimum");
    }

    @Test
    void checkInvestment_accreditedOnlyWithoutKyc_blocks() {
        when(policyRepository.findByJurisdiction("KG")).thenReturn(Optional.of(kgPolicy));
        when(complianceRecordRepository.findByInvestorId(investorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkInvestment(investorId, "KG", new BigDecimal("5000.00")))
                .isInstanceOf(ComplianceBlockedException.class)
                .hasMessageContaining("Accredited investors only");
    }

    @Test
    void checkInvestment_validAmount_allows() {
        when(policyRepository.findByJurisdiction("KG")).thenReturn(Optional.of(kgPolicy));
        ComplianceRecord record = ComplianceRecord.builder()
                .investorId(investorId)
                .walletAddress("0xabc")
                .status(ComplianceRecord.ComplianceStatus.APPROVED)
                .build();
        when(complianceRecordRepository.findByInvestorId(investorId)).thenReturn(Optional.of(record));

        var result = service.checkInvestment(investorId, "KG", new BigDecimal("5000.00"));

        assertThat(result.allowed()).isTrue();
        assertThat(result.minInvestmentUsd()).isEqualByComparingTo("1000.00");
    }
}
