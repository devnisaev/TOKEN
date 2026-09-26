package com.tokenrealty.indexer.service;

import com.tokenrealty.indexer.client.IssuanceClient;
import com.tokenrealty.indexer.entity.ReconciliationMismatch;
import com.tokenrealty.indexer.metrics.IndexerMetrics;
import com.tokenrealty.indexer.repository.ReconciliationMismatchRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceRemediationService {

    private final ReconciliationMismatchRepository mismatchRepository;
    private final IssuanceClient issuanceClient;
    private final IndexerMetrics indexerMetrics;

    @Transactional
    public ReconciliationMismatch remediate(UUID mismatchId) {
        ReconciliationMismatch mismatch = mismatchRepository.findById(mismatchId)
                .orElseThrow(() -> new ResourceNotFoundException("ReconciliationMismatch", mismatchId));
        if (mismatch.isResolved()) {
            throw new ValidationException("Mismatch already resolved: " + mismatchId);
        }
        issuanceClient.syncHolderBalance(
                mismatch.getContractId(),
                mismatch.getWalletAddress(),
                mismatch.getChainBalance());
        mismatch.setResolved(true);
        mismatchRepository.save(mismatch);
        indexerMetrics.recordReconciliationRemediated();
        indexerMetrics.setOpenMismatches(mismatchRepository.countByResolvedFalse());
        log.info("Remediated balance mismatch {} contract={} wallet={} chainBalance={}",
                mismatchId, mismatch.getContractId(), mismatch.getWalletAddress(), mismatch.getChainBalance());
        return mismatch;
    }
}
