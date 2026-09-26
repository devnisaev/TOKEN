package com.tokenrealty.indexer.service;

import com.tokenrealty.indexer.blockchain.OnChainBalanceReader;
import com.tokenrealty.indexer.client.IssuanceClient;
import com.tokenrealty.indexer.entity.ReconciliationMismatch;
import com.tokenrealty.indexer.kafka.outbox.OutboxBalanceMismatchPublisher;
import com.tokenrealty.indexer.metrics.IndexerMetrics;
import com.tokenrealty.indexer.repository.ReconciliationMismatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceReconciliationService {

    private final IssuanceClient issuanceClient;
    private final OnChainBalanceReader balanceReader;
    private final ReconciliationMismatchRepository mismatchRepository;
    private final OutboxBalanceMismatchPublisher mismatchPublisher;
    private final IndexerMetrics indexerMetrics;

    @Value("${tokenrealty.indexer.enabled:true}")
    private boolean indexerEnabled;

    @Scheduled(cron = "${tokenrealty.indexer.reconciliation.cron:0 30 */6 * * *}")
    @Transactional
    public void reconcileHolderBalances() {
        if (!indexerEnabled) {
            return;
        }
        int mismatches = 0;
        for (IssuanceClient.TokenContractView contract : issuanceClient.listContracts()) {
            if (contract.contractAddress() == null || contract.contractAddress().isBlank()) {
                continue;
            }
            mismatches += reconcileContract(contract);
        }
        indexerMetrics.setOpenMismatches(mismatchRepository.countByResolvedFalse());
        if (mismatches > 0) {
            log.warn("Balance reconciliation found {} mismatch(es)", mismatches);
        } else {
            log.debug("Balance reconciliation complete — no mismatches");
        }
    }

    private int reconcileContract(IssuanceClient.TokenContractView contract) {
        int count = 0;
        List<IssuanceClient.TokenHolderView> holders = issuanceClient.listHolders(contract.id());
        for (IssuanceClient.TokenHolderView holder : holders) {
            if (holder.balance() <= 0) {
                continue;
            }
            long chainBalance = balanceReader.balanceOf(contract.contractAddress(), holder.walletAddress());
            if (chainBalance != holder.balance()) {
                count++;
                var mismatch = mismatchRepository.save(ReconciliationMismatch.builder()
                        .contractId(contract.id())
                        .contractAddress(contract.contractAddress().toLowerCase())
                        .walletAddress(holder.walletAddress().toLowerCase())
                        .dbBalance(holder.balance())
                        .chainBalance(chainBalance)
                        .build());
                mismatchPublisher.publishMismatch(
                        mismatch.getId(),
                        contract.id(),
                        contract.contractAddress(),
                        holder.walletAddress(),
                        holder.balance(),
                        chainBalance);
                log.warn("Balance mismatch contract={} wallet={} db={} chain={}",
                        contract.id(), holder.walletAddress(), holder.balance(), chainBalance);
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationMismatch> openMismatches() {
        return mismatchRepository.findTop50ByResolvedFalseOrderByCreatedAtDesc();
    }
}
