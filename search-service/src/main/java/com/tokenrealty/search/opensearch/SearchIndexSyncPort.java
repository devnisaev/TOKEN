package com.tokenrealty.search.opensearch;

import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;

public interface SearchIndexSyncPort {

    void syncListing(ListingIndex listing);

    void syncBuilding(BuildingIndex building);
}
