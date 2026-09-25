package com.tokenrealty.marketplace.controller;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/listings")
@RequiredArgsConstructor
@Tag(name = "Listings", description = "Token property listings")
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    @Operation(summary = "Search listings")
    public Page<ListingResponse> list(
            @RequestParam(required = false) Listing.ListingStatus status,
            @RequestParam(required = false) UUID flatId,
            @PageableDefault(size = 20) Pageable pageable) {
        return listingService.findAll(status, flatId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get listing by id")
    public ListingResponse getById(@PathVariable UUID id) {
        return listingService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Create a listing for a tokenized flat")
    public ListingResponse create(@Valid @RequestBody CreateListingRequest request) {
        return listingService.create(request);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Cancel an active listing")
    public ListingResponse cancel(@PathVariable UUID id) {
        return listingService.cancel(id);
    }
}
