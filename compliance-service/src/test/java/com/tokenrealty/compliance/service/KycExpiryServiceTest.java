package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.entity.ComplianceRecord.ComplianceStatus;
import com.tokenrealty.compliance.kafka.port.KycEventPublisher;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KycExpiryService unit tests")
class KycExpiryServiceTest {

    @Mock ComplianceRecordRepository repository;
    @Mock KycEventPublisher kycEventPublisher;
    @InjectMocks KycExpiryService kycExpiryService;

    @Test
    @DisplayName("expireOverdueKyc marks records EXPIRED and publishes kyc-revoked")
    void expireOverdueKyc_revokesExpiredRecords() {
        UUID investorId = UUID.randomUUID();
        ComplianceRecord record = ComplianceRecord.builder()
                .investorId(investorId)
                .walletAddress("0xABC")
                .status(ComplianceStatus.APPROVED)
                .kycExpiresAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        when(repository.findByStatusAndKycExpiresAtBefore(eq(ComplianceStatus.APPROVED), any(Instant.class)))
                .thenReturn(List.of(record));
        when(repository.save(record)).thenReturn(record);

        kycExpiryService.expireOverdueKyc();

        assertThat(record.getStatus()).isEqualTo(ComplianceStatus.EXPIRED);
        assertThat(record.getRejectionReason()).isEqualTo("KYC expired");
        verify(kycEventPublisher).publishKycRevoked(
                eq(investorId), eq("0xABC"), eq("expired"), any(Instant.class));
    }

    @Test
    @DisplayName("expireOverdueKyc does nothing when no overdue records")
    void expireOverdueKyc_noOpWhenEmpty() {
        when(repository.findByStatusAndKycExpiresAtBefore(eq(ComplianceStatus.APPROVED), any(Instant.class)))
                .thenReturn(List.of());

        kycExpiryService.expireOverdueKyc();

        verify(repository, never()).save(any());
        verifyNoInteractions(kycEventPublisher);
    }
}
