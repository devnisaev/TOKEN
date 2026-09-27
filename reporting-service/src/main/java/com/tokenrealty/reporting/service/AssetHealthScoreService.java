package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.AssetHealthScoreItem;
import com.tokenrealty.reporting.dto.ReportingDtos.RecordAssetHealthScoreRequest;
import com.tokenrealty.reporting.entity.AssetHealthScoreRecord;
import com.tokenrealty.reporting.repository.AssetHealthScoreRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetHealthScoreService {

    private static final BigDecimal ESG_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal OCCUPANCY_WEIGHT = new BigDecimal("0.35");
    private static final BigDecimal INSURANCE_WEIGHT = new BigDecimal("0.25");

    private final AssetHealthScoreRecordRepository assetHealthScoreRecordRepository;

    public List<AssetHealthScoreItem> listByBuilding(UUID buildingId) {
        return assetHealthScoreRecordRepository.findByBuildingIdOrderByComputedAtDesc(buildingId).stream()
                .map(this::toItem)
                .toList();
    }

    public List<AssetHealthScoreItem> listAll() {
        return assetHealthScoreRecordRepository.findAllByOrderByComputedAtDesc().stream()
                .map(this::toItem)
                .toList();
    }

    public List<AssetHealthScoreItem> listHistoryByFlat(UUID flatId) {
        return assetHealthScoreRecordRepository.findByFlatIdOrderByComputedAtDesc(flatId).stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public AssetHealthScoreItem record(RecordAssetHealthScoreRequest request) {
        BigDecimal healthScore = compositeScore(
                request.esgFactor(), request.occupancyFactor(), request.insuranceFactor());
        AssetHealthScoreRecord record = assetHealthScoreRecordRepository.save(AssetHealthScoreRecord.builder()
                .flatId(request.flatId())
                .buildingId(request.buildingId())
                .healthScore(healthScore)
                .esgFactor(request.esgFactor())
                .occupancyFactor(request.occupancyFactor())
                .insuranceFactor(request.insuranceFactor())
                .computedAt(Instant.now())
                .build());
        return toItem(record);
    }

    static BigDecimal compositeScore(
            BigDecimal esgFactor, BigDecimal occupancyFactor, BigDecimal insuranceFactor) {
        return esgFactor.multiply(ESG_WEIGHT)
                .add(occupancyFactor.multiply(OCCUPANCY_WEIGHT))
                .add(insuranceFactor.multiply(INSURANCE_WEIGHT))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private AssetHealthScoreItem toItem(AssetHealthScoreRecord record) {
        return new AssetHealthScoreItem(
                record.getId(),
                record.getFlatId(),
                record.getBuildingId(),
                record.getHealthScore(),
                record.getEsgFactor(),
                record.getOccupancyFactor(),
                record.getInsuranceFactor(),
                record.getComputedAt());
    }
}
