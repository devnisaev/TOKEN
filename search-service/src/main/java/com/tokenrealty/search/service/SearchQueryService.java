package com.tokenrealty.search.service;

import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;
import com.tokenrealty.search.opensearch.OpenSearchQueryBackend;
import com.tokenrealty.search.repository.BuildingIndexRepository;
import com.tokenrealty.search.repository.ListingIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchQueryService {

    private final ListingIndexRepository listingIndexRepository;
    private final BuildingIndexRepository buildingIndexRepository;
    private final Optional<OpenSearchQueryBackend> openSearchQueryBackend;

    public Page<ListingIndex> searchListings(
            String q,
            String listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        if (openSearchQueryBackend.isPresent()) {
            return openSearchQueryBackend.get().searchListings(
                    normalizeQuery(q),
                    normalizeFilter(listingType),
                    minPrice,
                    maxPrice,
                    pageable);
        }
        return listingIndexRepository.search(
                normalizeQuery(q),
                normalizeFilter(listingType),
                minPrice,
                maxPrice,
                pageable);
    }

    public Page<BuildingIndex> searchBuildings(String q, Pageable pageable) {
        if (openSearchQueryBackend.isPresent()) {
            return openSearchQueryBackend.get().searchBuildings(normalizeQuery(q), pageable);
        }
        return buildingIndexRepository.search(normalizeQuery(q), pageable);
    }

    private static String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
