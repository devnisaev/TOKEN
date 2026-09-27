package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.RentCollectionSummary;
import com.tokenrealty.reporting.repository.RentCollectedRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentCollectionSummaryService {

    private final RentCollectedRecordRepository rentCollectedRecordRepository;
    private final Clock clock;

    public RentCollectionSummary summary() {
        return new RentCollectionSummary(
                rentCollectedRecordRepository.sumCollectedUsd(),
                rentCollectedRecordRepository.count(),
                rentCollectedRecordRepository.countDistinctOccupiedFlats(),
                clock.instant());
    }
}
