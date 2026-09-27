package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.EsgSnapshotItem;
import com.tokenrealty.reporting.dto.ReportingDtos.RecordEsgSnapshotRequest;
import com.tokenrealty.reporting.entity.EsgSnapshotRecord;
import com.tokenrealty.reporting.repository.EsgSnapshotRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EsgSnapshotService {

    private final EsgSnapshotRecordRepository esgSnapshotRecordRepository;

    public List<EsgSnapshotItem> listByBuilding(UUID buildingId) {
        return esgSnapshotRecordRepository.findByBuildingIdOrderBySnapshotAtDesc(buildingId).stream()
                .map(this::toItem)
                .toList();
    }

    public List<EsgSnapshotItem> listAll() {
        return esgSnapshotRecordRepository.findAll().stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public EsgSnapshotItem record(RecordEsgSnapshotRequest request) {
        EsgSnapshotRecord record = esgSnapshotRecordRepository.save(EsgSnapshotRecord.builder()
                .flatId(request.flatId())
                .buildingId(request.buildingId())
                .carbonScore(request.carbonScore())
                .energyRating(request.energyRating())
                .environmentalRiskTier(request.environmentalRiskTier())
                .occupancyPct(request.occupancyPct())
                .snapshotAt(Instant.now())
                .build());
        return toItem(record);
    }

    private EsgSnapshotItem toItem(EsgSnapshotRecord record) {
        return new EsgSnapshotItem(
                record.getId(),
                record.getFlatId(),
                record.getBuildingId(),
                record.getCarbonScore(),
                record.getEnergyRating(),
                record.getEnvironmentalRiskTier(),
                record.getOccupancyPct(),
                record.getSnapshotAt());
    }
}
