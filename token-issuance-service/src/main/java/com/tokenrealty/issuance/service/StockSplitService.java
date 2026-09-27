package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.kafka.command.StockSplitRequestedCommand;
import com.tokenrealty.issuance.kafka.port.StockSplitCompletedPublisher;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSplitService {

    private final TokenContractRepository contractRepository;
    private final TokenHolderRepository holderRepository;
    private final StockSplitCompletedPublisher stockSplitCompletedPublisher;
    private final Clock clock;

    @Transactional
    public void applySplit(StockSplitRequestedCommand command) {
        if (command.splitRatio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Split ratio must be positive");
        }

        TokenContract contract = contractRepository.findByFlatId(command.flatId())
                .orElseThrow(() -> new ResourceNotFoundException("TokenContract for flat", command.flatId()));

        BigDecimal ratio = command.splitRatio();
        long newTotalSupply = ratio.multiply(BigDecimal.valueOf(contract.getTotalSupply()))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        BigDecimal newTokenPrice = contract.getTokenPriceUsd()
                .divide(ratio, 2, RoundingMode.HALF_UP);

        contract.setTotalSupply(newTotalSupply);
        contract.setTokenPriceUsd(newTokenPrice);
        contractRepository.save(contract);

        List<TokenHolder> holders = holderRepository.findByTokenContractId(contract.getId());
        for (TokenHolder holder : holders) {
            long newBalance = ratio.multiply(BigDecimal.valueOf(holder.getBalance()))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();
            holder.setBalance(newBalance);
            holder.setBalanceUsd(newTokenPrice.multiply(BigDecimal.valueOf(newBalance)));
            holder.setOwnershipPercentage(ownershipPct(newBalance, newTotalSupply));
            holderRepository.save(holder);
        }

        Instant completedAt = clock.instant();
        stockSplitCompletedPublisher.publishStockSplitCompleted(
                new StockSplitCompletedPublisher.StockSplitCompletedEvent(
                        command.corporateActionId(),
                        contract.getId(),
                        command.flatId(),
                        command.period(),
                        ratio,
                        newTotalSupply,
                        newTokenPrice,
                        completedAt));
    }

    private static BigDecimal ownershipPct(long balance, long totalSupply) {
        if (totalSupply <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(balance)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSupply), 4, RoundingMode.HALF_UP);
    }
}
