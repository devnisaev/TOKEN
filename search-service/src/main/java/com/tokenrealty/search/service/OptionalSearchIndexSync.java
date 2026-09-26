package com.tokenrealty.search.service;

import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;
import com.tokenrealty.search.opensearch.SearchIndexSyncPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OptionalSearchIndexSync {

    private final Optional<SearchIndexSyncPort> searchIndexSyncPort;

    public void syncListing(ListingIndex listing) {
        searchIndexSyncPort.ifPresent(port -> port.syncListing(listing));
    }

    public void syncBuilding(BuildingIndex building) {
        searchIndexSyncPort.ifPresent(port -> port.syncBuilding(building));
    }
}
