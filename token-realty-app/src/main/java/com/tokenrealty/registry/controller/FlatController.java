package com.tokenrealty.registry.controller;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.service.FlatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Flats", description = "Flat registry operations")
public class FlatController {

    private final FlatService flatService;

    @GetMapping("/buildings/{buildingId}/flats")
    @Operation(summary = "List all flats in a building")
    public Page<FlatResponse> listByBuilding(
            @PathVariable UUID buildingId,
            @PageableDefault(size = 20) Pageable pageable) {
        return flatService.findByBuilding(buildingId, pageable);
    }

    @GetMapping("/buildings/{buildingId}/flats/available")
    @Operation(summary = "List available (not-yet-tokenized) flats in a building")
    public List<FlatResponse> listAvailable(@PathVariable UUID buildingId) {
        return flatService.findAvailableByBuilding(buildingId);
    }

    @GetMapping("/flats/{id}")
    @Operation(summary = "Get flat details including current valuation")
    public FlatResponse getById(@PathVariable UUID id) {
        return flatService.findById(id);
    }

    @PostMapping("/buildings/{buildingId}/flats")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Add a flat to a building")
    public FlatResponse create(
            @PathVariable UUID buildingId,
            @Valid @RequestBody CreateFlatRequest request) {
        return flatService.create(buildingId, request);
    }

    @PutMapping("/flats/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Update flat details")
    public FlatResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFlatRequest request) {
        return flatService.update(id, request);
    }

    @PatchMapping("/flats/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update flat status")
    public FlatResponse updateStatus(
            @PathVariable UUID id,
            @RequestParam Flat.FlatStatus status) {
        return flatService.updateStatus(id, status);
    }

    @PatchMapping("/flats/{id}/token-info")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set token contract info once minted on-chain")
    public FlatResponse setTokenInfo(
            @PathVariable UUID id,
            @RequestParam String contractAddress,
            @RequestParam Long totalTokens,
            @RequestParam BigDecimal tokenPriceUsd) {
        return flatService.setTokenInfo(id, contractAddress, totalTokens, tokenPriceUsd);
    }

    @DeleteMapping("/flats/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a flat (only if not tokenized)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        flatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
