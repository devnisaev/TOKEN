package com.tokenrealty.search.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "tokenrealty.search.backend", havingValue = "opensearch")
@RequiredArgsConstructor
public class OpenSearchIndexSync implements SearchIndexSyncPort {

    private final RestClient openSearchRestClient;
    private final OpenSearchProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public void syncListing(ListingIndex listing) {
        upsert(properties.listingsIndex(), listing.getListingId().toString(), toListingDocument(listing));
    }

    @Override
    public void syncBuilding(BuildingIndex building) {
        upsert(properties.buildingsIndex(), building.getBuildingId().toString(), toBuildingDocument(building));
    }

    private void upsert(String index, String id, Map<String, Object> document) {
        try {
            String body = objectMapper.writeValueAsString(document);
            openSearchRestClient.put()
                    .uri("/{index}/_doc/{id}", index, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sync document to OpenSearch index " + index, ex);
        }
    }

    private Map<String, Object> toListingDocument(ListingIndex listing) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("listingId", listing.getListingId().toString());
        document.put("flatId", listing.getFlatId() != null ? listing.getFlatId().toString() : null);
        document.put("buildingId", listing.getBuildingId() != null ? listing.getBuildingId().toString() : null);
        document.put("buildingName", listing.getBuildingName());
        document.put("city", listing.getCity());
        document.put("listingType", listing.getListingType());
        document.put("priceUsd", listing.getPriceUsd());
        document.put("navPerTokenUsd", listing.getNavPerTokenUsd());
        document.put("searchText", listing.getSearchText());
        document.put("indexedAt", listing.getIndexedAt());
        return document;
    }

    private Map<String, Object> toBuildingDocument(BuildingIndex building) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("buildingId", building.getBuildingId().toString());
        document.put("buildingName", building.getBuildingName());
        document.put("city", building.getCity());
        document.put("approvedAt", building.getApprovedAt());
        document.put("flatCount", building.getFlatCount());
        document.put("latestTokenPriceUsd", building.getLatestTokenPriceUsd());
        document.put("latestNavPerTokenUsd", building.getLatestNavPerTokenUsd());
        document.put("searchText", building.getSearchText());
        document.put("indexedAt", building.getIndexedAt());
        return document;
    }
}
