package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.RecomputeAssetHealthResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.RecordAssetHealthScoreRequest;
import com.tokenrealty.reporting.entity.EsgSnapshotRecord;
import com.tokenrealty.reporting.repository.EsgSnapshotRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetHealthRecomputeService {

    private static final BigDecimal DEFAULT_INSURANCE_FACTOR = new BigDecimal("100.00");
    private static final BigDecimal CARBON_BASELINE = new BigDecimal("100.00");

    private final EsgSnapshotRecordRepository esgSnapshotRecordRepository;
    private final AssetHealthScoreService assetHealthScoreService;

    @Transactional
    public RecomputeAssetHealthResponse recomputeFromEsgSnapshots() {
        Map<UUID, EsgSnapshotRecord> latestByFlat = latestSnapshotPerFlat();
        int recomputed = 0;
        for (EsgSnapshotRecord snapshot : latestByFlat.values()) {
            assetHealthScoreService.record(toHealthRequest(snapshot));
            recomputed++;
        }
        return new RecomputeAssetHealthResponse(recomputed);
    }

    private Map<UUID, EsgSnapshotRecord> latestSnapshotPerFlat() {
        List<EsgSnapshotRecord> snapshots = esgSnapshotRecordRepository.findAll();
        Map<UUID, EsgSnapshotRecord> latest = new HashMap<>();
        for (EsgSnapshotRecord snapshot : snapshots) {
            if (snapshot.getFlatId() == null) {
                continue;
            }
            latest.merge(
                    snapshot.getFlatId(),
                    snapshot,
                    (existing, candidate) -> Comparator.comparing(EsgSnapshotRecord::getSnapshotAt)
                            .compare(existing, candidate) >= 0
                            ? existing
                            : candidate);
        }
        return latest;
    }

    private RecordAssetHealthScoreRequest toHealthRequest(EsgSnapshotRecord snapshot) {
        BigDecimal esgFactor = esgFactorFromCarbon(snapshot.getCarbonScore());
        BigDecimal occupancyFactor = snapshot.getOccupancyPct() != null
                ? snapshot.getOccupancyPct().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return new RecordAssetHealthScoreRequest(
                snapshot.getFlatId(),
                snapshot.getBuildingId(),
                esgFactor,
                occupancyFactor,
                DEFAULT_INSURANCE_FACTOR);
    }

    static BigDecimal esgFactorFromCarbon(BigDecimal carbonScore) {
        if (carbonScore == null) {
            return new BigDecimal("50.00");
        }
        BigDecimal factor = CARBON_BASELINE.subtract(carbonScore);
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (factor.compareTo(CARBON_BASELINE) > 0) {
            return CARBON_BASELINE.setScale(2, RoundingMode.HALF_UP);
        }
        return factor.setScale(2, RoundingMode.HALF_UP);
    }
}
