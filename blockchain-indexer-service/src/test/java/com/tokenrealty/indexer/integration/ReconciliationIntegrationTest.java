package com.tokenrealty.indexer.integration;

import com.tokenrealty.indexer.blockchain.OnChainBalanceReader;
import com.tokenrealty.indexer.client.IssuanceClient;
import com.tokenrealty.indexer.kafka.outbox.OutboxBalanceMismatchPublisher;
import com.tokenrealty.indexer.repository.ReconciliationMismatchRepository;
import com.tokenrealty.indexer.service.BalanceReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Balance reconciliation integration test")
class ReconciliationIntegrationTest {

    @Autowired BalanceReconciliationService reconciliationService;
    @Autowired ReconciliationMismatchRepository mismatchRepository;

    @MockitoBean IssuanceClient issuanceClient;
    @MockitoBean OnChainBalanceReader balanceReader;
    @MockitoBean OutboxBalanceMismatchPublisher mismatchPublisher;

    @BeforeEach
    void enableIndexer() {
        ReflectionTestUtils.setField(reconciliationService, "indexerEnabled", true);
        mismatchRepository.deleteAll();
    }

    @Test
    @DisplayName("reconcileHolderBalances persists mismatch and exposes via openMismatches")
    void reconcileHolderBalances_persistsMismatch() {
        UUID contractId = UUID.randomUUID();
        var contract = new IssuanceClient.TokenContractView(
                contractId, UUID.randomUUID(), "0xCONTRACT", "SFT-101", 1000L);
        var holder = new IssuanceClient.TokenHolderView(
                UUID.randomUUID(), contractId, UUID.randomUUID(), "0xWALLET", 100L);

        when(issuanceClient.listContracts()).thenReturn(List.of(contract));
        when(issuanceClient.listHolders(contractId)).thenReturn(List.of(holder));
        when(balanceReader.balanceOf("0xCONTRACT", "0xWALLET")).thenReturn(90L);

        reconciliationService.reconcileHolderBalances();

        assertThat(mismatchRepository.countByResolvedFalse()).isEqualTo(1);
        assertThat(reconciliationService.openMismatches()).hasSize(1);
        assertThat(reconciliationService.openMismatches().getFirst().getDbBalance()).isEqualTo(100L);
        assertThat(reconciliationService.openMismatches().getFirst().getChainBalance()).isEqualTo(90L);
        verify(mismatchPublisher).publishMismatch(any(), any(), anyString(), anyString(), anyLong(), anyLong());
    }
}
