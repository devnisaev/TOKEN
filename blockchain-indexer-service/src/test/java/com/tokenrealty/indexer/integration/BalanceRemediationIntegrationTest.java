package com.tokenrealty.indexer.integration;

import com.tokenrealty.indexer.client.IssuanceClient;
import com.tokenrealty.indexer.entity.ReconciliationMismatch;
import com.tokenrealty.indexer.repository.ReconciliationMismatchRepository;
import com.tokenrealty.indexer.service.BalanceRemediationService;
import com.tokenrealty.web.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Balance remediation integration test")
class BalanceRemediationIntegrationTest {

    @Autowired BalanceRemediationService remediationService;
    @Autowired ReconciliationMismatchRepository mismatchRepository;

    @MockitoBean IssuanceClient issuanceClient;

    @BeforeEach
    void clean() {
        mismatchRepository.deleteAll();
    }

    @Test
    @DisplayName("remediate syncs holder balance and marks mismatch resolved")
    void remediate_syncsAndResolves() {
        UUID contractId = UUID.randomUUID();
        var mismatch = mismatchRepository.save(ReconciliationMismatch.builder()
                .contractId(contractId)
                .contractAddress("0xcontract")
                .walletAddress("0xwallet")
                .dbBalance(100L)
                .chainBalance(90L)
                .build());

        var resolved = remediationService.remediate(mismatch.getId());

        assertThat(resolved.isResolved()).isTrue();
        assertThat(mismatchRepository.countByResolvedFalse()).isZero();
        verify(issuanceClient).syncHolderBalance(eq(contractId), eq("0xwallet"), eq(90L));
    }

    @Test
    @DisplayName("remediate rejects already resolved mismatch")
    void remediate_rejectsResolved() {
        var mismatch = mismatchRepository.save(ReconciliationMismatch.builder()
                .contractId(UUID.randomUUID())
                .contractAddress("0xcontract")
                .walletAddress("0xwallet")
                .dbBalance(100L)
                .chainBalance(90L)
                .resolved(true)
                .build());

        assertThatThrownBy(() -> remediationService.remediate(mismatch.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already resolved");
    }
}
