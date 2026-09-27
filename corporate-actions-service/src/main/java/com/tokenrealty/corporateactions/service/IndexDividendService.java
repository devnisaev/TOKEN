package com.tokenrealty.corporateactions.service;

import com.tokenrealty.corporateactions.entity.IndexConstituent;
import com.tokenrealty.corporateactions.entity.IndexDefinition;
import com.tokenrealty.corporateactions.entity.IndexDividendAccrual;
import com.tokenrealty.corporateactions.kafka.command.PayoutCompletedCommand;
import com.tokenrealty.corporateactions.repository.IndexConstituentRepository;
import com.tokenrealty.corporateactions.repository.IndexDefinitionRepository;
import com.tokenrealty.corporateactions.repository.IndexDividendAccrualRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexDividendService {

    private final IndexDefinitionRepository indexDefinitionRepository;
    private final IndexConstituentRepository indexConstituentRepository;
    private final IndexDividendAccrualRepository indexDividendAccrualRepository;

    @Transactional
    public void onPayoutCompleted(PayoutCompletedCommand command) {
        if (command.contractId() == null || command.amountUsd() == null) {
            return;
        }
        if (indexDividendAccrualRepository.existsBySourcePayoutId(command.payoutId())) {
            return;
        }
        List<IndexConstituent> matches = indexConstituentRepository.findByContractId(command.contractId());
        for (IndexConstituent constituent : matches) {
            IndexDefinition index = indexDefinitionRepository.findById(constituent.getIndexId()).orElse(null);
            if (index == null || index.getStatus() != IndexDefinition.IndexStatus.ACTIVE) {
                continue;
            }
            BigDecimal indexShare = command.amountUsd()
                    .multiply(BigDecimal.valueOf(constituent.getWeightBps()))
                    .divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_UP);
            indexDividendAccrualRepository.save(IndexDividendAccrual.builder()
                    .indexId(constituent.getIndexId())
                    .constituentContractId(command.contractId())
                    .sourcePayoutId(command.payoutId())
                    .constituentPayoutUsd(command.amountUsd())
                    .indexShareUsd(indexShare)
                    .weightBps(constituent.getWeightBps())
                    .build());
        }
    }
}
