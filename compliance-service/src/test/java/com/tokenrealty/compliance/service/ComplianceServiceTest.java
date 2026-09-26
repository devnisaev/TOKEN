package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.dto.ComplianceDtos.*;
import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.kafka.port.KycEventPublisher;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import com.tokenrealty.web.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplianceService unit tests")
class ComplianceServiceTest {

    @Mock ComplianceRecordRepository repository;
    @Mock KycEventPublisher kycEventPublisher;
    @InjectMocks ComplianceService service;

    private UUID investorId;
    private ComplianceRecord record;

    @BeforeEach
    void setUp() {
        investorId = UUID.randomUUID();
        record = ComplianceRecord.builder()
                .investorId(investorId)
                .walletAddress("0xinvestorwallet123")
                .fullName("Test Investor")
                .countryCode("KG")
                .status(ComplianceRecord.ComplianceStatus.PENDING)
                .build();
        record.setId(UUID.randomUUID());
    }

    @Test
    void register_success() {
        var request = new RegisterComplianceRequest(
                investorId, "0xInvestorWallet123", "Test Investor", "KG", "Sumsub", "REF-1");
        when(repository.existsByWalletAddress("0xinvestorwallet123")).thenReturn(false);
        when(repository.existsByInvestorId(investorId)).thenReturn(false);
        when(repository.save(any())).thenReturn(record);

        var result = service.register(request);

        assertThat(result).isNotNull();
        verify(repository).save(any(ComplianceRecord.class));
    }

    @Test
    void register_duplicateWallet() {
        when(repository.existsByWalletAddress("0xinvestorwallet123")).thenReturn(true);
        var request = new RegisterComplianceRequest(
                investorId, "0xInvestorWallet123", "Test", "KG", "Sumsub", "REF-1");

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void verify_publishesKycApproved() {
        when(repository.findById(any())).thenReturn(java.util.Optional.of(record));
        when(repository.save(any())).thenReturn(record);
        Instant expiresAt = Instant.now().plusSeconds(86400 * 365);

        service.verify(record.getId(), new ComplianceVerifyRequest(expiresAt));

        verify(kycEventPublisher).publishKycApproved(
                eq(investorId), eq(record.getWalletAddress()), eq("KG"), eq(expiresAt), any(Instant.class));
    }
}
