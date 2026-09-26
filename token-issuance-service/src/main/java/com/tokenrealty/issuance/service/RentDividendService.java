package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.dto.IssuanceDtos.DistributeDividendRequest;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.web.exception.ResourceNotFoundException;
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
        TokenContract contract = contractRepository.findByFlatId(command.flatId())
                .orElseThrow(() -> new ResourceNotFoundException("TokenContract for flat", command.flatId()));

        YearMonth yearMonth = YearMonth.parse(command.period());
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();

        dividendService.distribute(contract.getId(), new DistributeDividendRequest(
                periodStart, periodEnd, command.amountUsd()));
    }
}
