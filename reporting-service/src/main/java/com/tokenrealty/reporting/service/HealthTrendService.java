package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.HealthTrendItem;
import com.tokenrealty.reporting.entity.AssetHealthScoreRecord;
import com.tokenrealty.reporting.repository.AssetHealthScoreRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthTrendService {

    private final AssetHealthScoreRecordRepository assetHealthScoreRecordRepository;

    public List<HealthTrendItem> listDeclining() {
        Map<UUID, List<AssetHealthScoreRecord>> byFlat = new HashMap<>();
        for (AssetHealthScoreRecord score : assetHealthScoreRecordRepository.findAll()) {
            if (score.getFlatId() == null) {
                continue;
            }
            byFlat.computeIfAbsent(score.getFlatId(), ignored -> new ArrayList<>()).add(score);
        }

        List<HealthTrendItem> declining = new ArrayList<>();
        for (var entry : byFlat.entrySet()) {
            List<AssetHealthScoreRecord> scores = entry.getValue().stream()
                    .sorted(Comparator.comparing(AssetHealthScoreRecord::getComputedAt).reversed())
                    .toList();
            if (scores.size() < 2) {
                continue;
            }
            AssetHealthScoreRecord latest = scores.get(0);
            AssetHealthScoreRecord previous = scores.get(1);
            BigDecimal delta = latest.getHealthScore().subtract(previous.getHealthScore());
            if (delta.compareTo(BigDecimal.ZERO) >= 0) {
                continue;
            }
            declining.add(new HealthTrendItem(
                    entry.getKey(),
                    latest.getBuildingId(),
                    previous.getHealthScore(),
                    latest.getHealthScore(),
                    delta,
                    latest.getComputedAt()));
        }
        return declining.stream()
                .sorted(Comparator.comparing(HealthTrendItem::delta))
                .toList();
    }
}
