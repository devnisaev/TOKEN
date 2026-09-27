package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.PropertyRegistryClient;
import com.tokenrealty.gateway.client.ValuationClient;
import com.tokenrealty.gateway.dto.BffDtos.BuildingBffDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BffBuildingService {

    private final PropertyRegistryClient registryClient;
    private final ValuationClient valuationClient;

    public BuildingBffDetailResponse getBuildingDetail(UUID buildingId) {
        var building = registryClient.getBuilding(buildingId);
        int tokenized = 0;
        int available = 0;
        if (building.flats() != null) {
            for (var flat : building.flats()) {
                if ("TOKENIZED".equals(flat.status()) || "FULLY_SOLD".equals(flat.status())) {
                    tokenized++;
                }
                if ("AVAILABLE".equals(flat.status()) || "REGISTERED".equals(flat.status())) {
                    available++;
                }
            }
        }
        var valuations = valuationClient.listByBuilding(buildingId);
        long approvedCount = valuations.stream()
                .filter(v -> "APPROVED".equals(v.status()))
                .count();
        Instant latestApprovedAt = valuations.stream()
                .filter(v -> "APPROVED".equals(v.status()))
                .map(ValuationClient.ValuationRequestView::reviewedAt)
                .filter(at -> at != null)
                .max(Instant::compareTo)
                .orElse(null);

        return BuildingBffDetailResponse.builder()
                .building(building)
                .tokenizedFlatCount(tokenized)
                .availableFlatCount(available)
                .approvedValuationCount((int) approvedCount)
                .latestNavAttestedAt(latestApprovedAt)
                .build();
    }
}
