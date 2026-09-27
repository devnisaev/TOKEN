package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.OperatorKpiResponse;
import com.tokenrealty.reporting.entity.AssetHealthScoreRecord;
import com.tokenrealty.reporting.entity.EsgSnapshotRecord;
import com.tokenrealty.reporting.repository.AssetHealthScoreRecordRepository;
import com.tokenrealty.reporting.repository.EsgSnapshotRecordRepository;
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
@Transactional(readOnly = true)
public class OperatorKpiService {

    private static final BigDecimal AT_RISK_THRESHOLD = new BigDecimal("50.00");

    private final EsgSnapshotRecordRepository esgSnapshotRecordRepository;
    private final AssetHealthScoreRecordRepository assetHealthScoreRecordRepository;
    private final Clock clock;

    public OperatorKpiResponse dashboard() {
        Instant now = clock.instant();
        List<EsgSnapshotRecord> esgSnapshots = esgSnapshotRecordRepository.findAll();
        List<AssetHealthScoreRecord> healthScores = assetHealthScoreRecordRepository.findAll();

        BigDecimal averageOccupancy = average(esgSnapshots.stream()
                .map(EsgSnapshotRecord::getOccupancyPct)
                .toList());
        BigDecimal averageCarbon = average(esgSnapshots.stream()
                .map(EsgSnapshotRecord::getCarbonScore)
                .toList());
        BigDecimal averageHealth = average(healthScores.stream()
                .map(AssetHealthScoreRecord::getHealthScore)
                .toList());

        long atRiskCount = healthScores.stream()
                .filter(score -> score.getHealthScore().compareTo(AT_RISK_THRESHOLD) < 0)
                .count();

        return new OperatorKpiResponse(
                averageOccupancy,
                averageCarbon,
                averageHealth,
                healthScores.size(),
                atRiskCount,
                esgSnapshots.size(),
                now);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = values.stream()
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = values.stream().filter(value -> value != null).count();
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
