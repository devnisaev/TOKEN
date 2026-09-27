package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.BuildingHealthItem;
import com.tokenrealty.reporting.dto.ReportingDtos.PortfolioHealthItem;
import com.tokenrealty.reporting.entity.AssetHealthScoreRecord;
import com.tokenrealty.reporting.repository.AssetHealthScoreRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingHealthService {

    private static final BigDecimal AT_RISK_THRESHOLD = new BigDecimal("50.00");

    private final AssetHealthScoreRecordRepository assetHealthScoreRecordRepository;

    public List<BuildingHealthItem> listBuildingRollups() {
        Map<UUID, List<AssetHealthScoreRecord>> byBuilding = new HashMap<>();
        for (AssetHealthScoreRecord score : latestScorePerFlat()) {
            byBuilding.computeIfAbsent(score.getBuildingId(), ignored -> new ArrayList<>()).add(score);
        }
        return byBuilding.entrySet().stream()
                .map(entry -> toBuildingItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(BuildingHealthItem::averageHealthScore).reversed())
                .toList();
    }

    public BuildingHealthItem getBuildingRollup(UUID buildingId) {
        List<AssetHealthScoreRecord> scores = latestScorePerFlat().stream()
                .filter(score -> buildingId.equals(score.getBuildingId()))
                .toList();
        return toBuildingItem(buildingId, scores);
    }

    public List<PortfolioHealthItem> portfolioHealth(List<UUID> flatIds) {
        if (flatIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, AssetHealthScoreRecord> latestByFlat = latestScorePerFlat(flatIds);
        return flatIds.stream()
                .map(flatId -> {
                    AssetHealthScoreRecord score = latestByFlat.get(flatId);
                    if (score == null) {
                        return new PortfolioHealthItem(flatId, null, null, null, null);
                    }
                    return new PortfolioHealthItem(
                            flatId,
                            score.getBuildingId(),
                            score.getHealthScore(),
                            score.getEsgFactor(),
                            score.getOccupancyFactor());
                })
                .toList();
    }

    private List<AssetHealthScoreRecord> latestScorePerFlat() {
        return new ArrayList<>(latestScorePerFlatMap(assetHealthScoreRecordRepository.findAll()).values());
    }

    private Map<UUID, AssetHealthScoreRecord> latestScorePerFlat(List<UUID> flatIds) {
        return latestScorePerFlatMap(assetHealthScoreRecordRepository.findByFlatIdIn(flatIds));
    }

    private Map<UUID, AssetHealthScoreRecord> latestScorePerFlatMap(List<AssetHealthScoreRecord> scores) {
        Map<UUID, AssetHealthScoreRecord> latest = new HashMap<>();
        for (AssetHealthScoreRecord score : scores) {
            if (score.getFlatId() == null) {
                continue;
            }
            latest.merge(
                    score.getFlatId(),
                    score,
                    (existing, candidate) -> Comparator.comparing(AssetHealthScoreRecord::getComputedAt)
                            .compare(existing, candidate) >= 0
                            ? existing
                            : candidate);
        }
        return latest;
    }

    private BuildingHealthItem toBuildingItem(UUID buildingId, List<AssetHealthScoreRecord> scores) {
        if (scores.isEmpty()) {
            return new BuildingHealthItem(buildingId, BigDecimal.ZERO, 0, 0);
        }
        BigDecimal average = scores.stream()
                .map(AssetHealthScoreRecord::getHealthScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        long atRisk = scores.stream()
                .filter(score -> score.getHealthScore().compareTo(AT_RISK_THRESHOLD) < 0)
                .count();
        return new BuildingHealthItem(buildingId, average, scores.size(), atRisk);
    }
}
