package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.dto.IssuanceDtos.DistributeDividendRequest;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.kafka.command.DividendDistributionRequestedCommand;
import com.tokenrealty.issuance.kafka.command.RentCollectedCommand;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentDividendService {

    private final TokenContractRepository contractRepository;
    private final DividendService dividendService;

    @Transactional
    public void distributeFromRent(RentCollectedCommand command) {
        distributeForFlat(command.flatId(), command.period(), command.amountUsd());
    }

    @Transactional
    public void distributeForFlat(DividendDistributionRequestedCommand command) {
        distributeForFlat(command.flatId(), command.period(), command.grossAmountUsd());
    }

    private void distributeForFlat(java.util.UUID flatId, String period, java.math.BigDecimal amountUsd) {
        TokenContract contract = contractRepository.findByFlatId(flatId)
                .orElseThrow(() -> new ResourceNotFoundException("TokenContract for flat", flatId));

        YearMonth yearMonth = YearMonth.parse(period);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();

        dividendService.distribute(contract.getId(), new DistributeDividendRequest(
                periodStart, periodEnd, amountUsd));
    }
}
