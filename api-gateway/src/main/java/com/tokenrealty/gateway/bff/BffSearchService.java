package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.SearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BffSearchService {

    private final SearchClient searchClient;

    public SearchClient.SpringPage<SearchClient.ListingSearchResult> searchListings(
            String q,
            String listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return searchClient.searchListings(q, listingType, minPrice, maxPrice, pageable);
    }

    public SearchClient.SpringPage<SearchClient.BuildingSearchResult> searchBuildings(String q, Pageable pageable) {
        return searchClient.searchBuildings(q, pageable);
    }
}
