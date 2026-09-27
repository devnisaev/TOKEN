package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.client.RentalClient;
import com.tokenrealty.reporting.client.RentalClient.LeaseExpiryView;
import com.tokenrealty.reporting.dto.ReportingDtos.LeaseCoverageSummary;
import com.tokenrealty.reporting.dto.ReportingDtos.LeaseExpiryItem;
import com.tokenrealty.reporting.entity.EsgSnapshotRecord;
import com.tokenrealty.reporting.repository.EsgSnapshotRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaseCoverageService {

    private static final BigDecimal VACANCY_OCCUPANCY_THRESHOLD = new BigDecimal("60.00");

    private final RentalClient rentalClient;
    private final EsgSnapshotRecordRepository esgSnapshotRecordRepository;
    private final Clock clock;

    public LeaseCoverageSummary summary(int expiringWithinDays) {
        Set<UUID> leasedFlats = rentalClient.activeLeaseFlatIds();
        List<LeaseExpiryView> expiring = rentalClient.listExpiringLeases(expiringWithinDays);
        long vacancyRiskCount = countVacancyRiskFlats(leasedFlats);
        return new LeaseCoverageSummary(
                leasedFlats.size(),
                expiring.size(),
                vacancyRiskCount,
                expiring.stream().map(this::toItem).toList(),
                clock.instant());
    }

    public List<UUID> vacancyRiskFlatIds() {
        Set<UUID> leasedFlats = rentalClient.activeLeaseFlatIds();
        return latestLowOccupancyFlats().stream()
                .filter(flatId -> !leasedFlats.contains(flatId))
                .toList();
    }

    long countVacancyRiskFlats(Set<UUID> leasedFlats) {
        return latestLowOccupancyFlats().stream()
                .filter(flatId -> !leasedFlats.contains(flatId))
                .count();
    }

    private List<UUID> latestLowOccupancyFlats() {
        Map<UUID, EsgSnapshotRecord> latestByFlat = new HashMap<>();
        for (EsgSnapshotRecord snapshot : esgSnapshotRecordRepository.findAll()) {
            if (snapshot.getFlatId() == null) {
                continue;
            }
            latestByFlat.merge(
                    snapshot.getFlatId(),
                    snapshot,
                    (existing, candidate) -> Comparator.comparing(EsgSnapshotRecord::getSnapshotAt)
                            .compare(existing, candidate) >= 0
                            ? existing
                            : candidate);
        }
        return latestByFlat.values().stream()
                .filter(snapshot -> snapshot.getOccupancyPct() != null)
                .filter(snapshot -> snapshot.getOccupancyPct().compareTo(VACANCY_OCCUPANCY_THRESHOLD) < 0)
                .map(EsgSnapshotRecord::getFlatId)
                .toList();
    }

    private LeaseExpiryItem toItem(LeaseExpiryView view) {
        return new LeaseExpiryItem(
                view.id(),
                view.flatId(),
                view.tenantId(),
                view.endDate(),
                view.daysUntilExpiry());
    }
}
