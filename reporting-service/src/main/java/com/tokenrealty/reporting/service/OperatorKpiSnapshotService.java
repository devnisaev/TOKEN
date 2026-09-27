package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.OperatorKpiResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.OperatorKpiSnapshotItem;
import com.tokenrealty.reporting.entity.OperatorKpiSnapshotRecord;
import com.tokenrealty.reporting.repository.OperatorKpiSnapshotRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperatorKpiSnapshotService {

    private final OperatorKpiSnapshotRecordRepository snapshotRecordRepository;
    private final OperatorKpiService operatorKpiService;
    private final Clock clock;

    public List<OperatorKpiSnapshotItem> listRecent() {
        return snapshotRecordRepository.findTop30ByOrderBySnapshotAtDesc().stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public OperatorKpiSnapshotItem recordNow() {
        OperatorKpiResponse kpis = operatorKpiService.dashboard();
        Instant now = clock.instant();
        OperatorKpiSnapshotRecord record = snapshotRecordRepository.save(OperatorKpiSnapshotRecord.builder()
                .averageOccupancyPct(kpis.averageOccupancyPct())
                .averageCarbonScore(kpis.averageCarbonScore())
                .averageHealthScore(kpis.averageHealthScore())
                .trackedAssetCount(kpis.trackedAssetCount())
                .atRiskAssetCount(kpis.atRiskAssetCount())
                .openOperatorAlertCount(kpis.openOperatorAlertCount())
                .openMaintenanceTicketCount(kpis.openMaintenanceTicketCount())
                .snapshotAt(now)
                .build());
        return toItem(record);
    }

    private OperatorKpiSnapshotItem toItem(OperatorKpiSnapshotRecord record) {
        return new OperatorKpiSnapshotItem(
                record.getId(),
                record.getAverageOccupancyPct(),
                record.getAverageCarbonScore(),
                record.getAverageHealthScore(),
                record.getTrackedAssetCount(),
                record.getAtRiskAssetCount(),
                record.getOpenOperatorAlertCount(),
                record.getOpenMaintenanceTicketCount(),
                record.getSnapshotAt());
    }
}
