package com.tokenrealty.search.service;

import com.tokenrealty.search.client.PropertyRegistryClient;
import com.tokenrealty.search.client.PropertyRegistryClient.BuildingView;
import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingEnrichmentService {

    private final PropertyRegistryClient propertyRegistryClient;

    public void enrichBuilding(BuildingIndex building, UUID buildingId) {
        propertyRegistryClient.findBuilding(buildingId).ifPresent(view -> apply(building, view));
    }

    public void enrichListings(List<ListingIndex> listings, UUID buildingId) {
        propertyRegistryClient.findBuilding(buildingId).ifPresent(view -> {
            for (ListingIndex listing : listings) {
                apply(listing, view);
            }
        });
    }

    private static void apply(BuildingIndex building, BuildingView view) {
        building.setBuildingName(view.name());
        building.setCity(view.city());
    }

    private static void apply(ListingIndex listing, BuildingView view) {
        listing.setBuildingName(view.name());
        listing.setCity(view.city());
    }
}
