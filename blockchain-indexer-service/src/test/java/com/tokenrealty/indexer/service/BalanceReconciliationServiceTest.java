package com.tokenrealty.indexer.service;

import com.tokenrealty.indexer.blockchain.OnChainBalanceReader;
import com.tokenrealty.indexer.client.IssuanceClient;
import com.tokenrealty.indexer.kafka.outbox.OutboxBalanceMismatchPublisher;
import com.tokenrealty.indexer.metrics.IndexerMetrics;
import com.tokenrealty.indexer.repository.ReconciliationMismatchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceReconciliationService unit tests")
class BalanceReconciliationServiceTest {

    @Mock IssuanceClient issuanceClient;
    @Mock OnChainBalanceReader balanceReader;
    @Mock ReconciliationMismatchRepository mismatchRepository;
    @Mock OutboxBalanceMismatchPublisher mismatchPublisher;
    @Mock IndexerMetrics indexerMetrics;
    @InjectMocks BalanceReconciliationService reconciliationService;

    @Test
    @DisplayName("reconcileHolderBalances records mismatch when chain differs from DB")
    void reconcileHolderBalances_recordsMismatch() {
        UUID contractId = UUID.randomUUID();
        var contract = new IssuanceClient.TokenContractView(
                contractId, UUID.randomUUID(), "0xCONTRACT", "TKN", 1000L);
        var holder = new IssuanceClient.TokenHolderView(
                UUID.randomUUID(), contractId, UUID.randomUUID(), "0xWALLET", 100L);

        when(issuanceClient.listContracts()).thenReturn(List.of(contract));
        when(issuanceClient.listHolders(contractId)).thenReturn(List.of(holder));
        when(balanceReader.balanceOf("0xCONTRACT", "0xWALLET")).thenReturn(90L);
        when(mismatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mismatchRepository.countByResolvedFalse()).thenReturn(1L);
        ReflectionTestUtils.setField(reconciliationService, "indexerEnabled", true);

        reconciliationService.reconcileHolderBalances();

        verify(mismatchRepository).save(any());
    }
}
