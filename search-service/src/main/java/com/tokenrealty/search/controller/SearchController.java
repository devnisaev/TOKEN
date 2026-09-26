package com.tokenrealty.search.controller;

import com.tokenrealty.search.dto.SearchDtos.BuildingSearchResult;
import com.tokenrealty.search.dto.SearchDtos.ListingSearchResult;
import com.tokenrealty.search.service.SearchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchQueryService queryService;

    @GetMapping("/listings")
    public Page<ListingSearchResult> searchListings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20) Pageable pageable) {
        return queryService.searchListings(q, listingType, minPrice, maxPrice, pageable)
                .map(ListingSearchResult::from);
    }

    @GetMapping("/buildings")
    public Page<BuildingSearchResult> searchBuildings(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return queryService.searchBuildings(q, pageable)
                .map(BuildingSearchResult::from);
    }
}
