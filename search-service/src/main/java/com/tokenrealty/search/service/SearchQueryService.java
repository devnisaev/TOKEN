package com.tokenrealty.search.service;

import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;
import com.tokenrealty.search.repository.BuildingIndexRepository;
import com.tokenrealty.search.repository.ListingIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchQueryService {

    private final ListingIndexRepository listingIndexRepository;
    private final BuildingIndexRepository buildingIndexRepository;

    public Page<ListingIndex> searchListings(
            String q,
            String listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return listingIndexRepository.search(
                normalizeQuery(q),
                normalizeFilter(listingType),
                minPrice,
                maxPrice,
                pageable);
    }

    public Page<BuildingIndex> searchBuildings(String q, Pageable pageable) {
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
