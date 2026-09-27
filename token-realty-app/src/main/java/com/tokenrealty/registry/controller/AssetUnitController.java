package com.tokenrealty.registry.controller;

import com.tokenrealty.registry.dto.PropertyDtos.AssetUnitResponse;
import com.tokenrealty.registry.dto.PropertyDtos.CreateFlatRequest;
import com.tokenrealty.registry.dto.PropertyDtos.UpdateFlatRequest;
import com.tokenrealty.registry.entity.Building;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/asset-units")
@RequiredArgsConstructor
@Tag(name = "Asset Units", description = "Universal asset unit API (Flat alias, Phase 12)")
public class AssetUnitController {

    private final FlatService flatService;

    @GetMapping
    @Operation(summary = "List asset units with optional category and liquidity filters")
    public Page<AssetUnitResponse> list(
            @RequestParam(required = false) Flat.OperatingModel operatingModel,
            @RequestParam(required = false) Flat.LiquidityTier liquidityTier,
            @RequestParam(required = false) Building.PropertyCategory propertyCategory,
            @PageableDefault(size = 20) Pageable pageable) {
        return flatService.findAssetUnits(operatingModel, liquidityTier, propertyCategory, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset unit by id")
    public AssetUnitResponse getById(@PathVariable UUID id) {
        return flatService.findAssetUnitById(id);
    }

    @PostMapping("/buildings/{buildingId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Register an asset unit in a building")
    public AssetUnitResponse create(
            @PathVariable UUID buildingId,
            @Valid @RequestBody CreateFlatRequest request) {
        return flatService.findAssetUnitById(flatService.create(buildingId, request).id());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Update asset unit metadata")
    public AssetUnitResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFlatRequest request) {
        return flatService.findAssetUnitById(flatService.update(id, request).id());
    }
}
