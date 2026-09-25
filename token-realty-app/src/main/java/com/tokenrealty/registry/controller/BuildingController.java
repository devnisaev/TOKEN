package com.tokenrealty.registry.controller;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.service.BuildingService;
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

import java.util.UUID;

@RestController
@RequestMapping("/v1/buildings")
@RequiredArgsConstructor
@Tag(name = "Buildings", description = "Building registry operations")
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping
    @Operation(summary = "List all buildings")
    public Page<BuildingResponse> listAll(
            @RequestParam(required = false) Building.BuildingStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {

        if (search != null && !search.isBlank()) {
            return buildingService.search(search, pageable);
        }
        if (status != null) {
            return buildingService.findByStatus(status, pageable);
        }
        return buildingService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get building detail with flats, SPV and documents")
    public BuildingDetailResponse getById(@PathVariable UUID id) {
        return buildingService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Register a new building")
    public BuildingResponse create(@Valid @RequestBody CreateBuildingRequest request) {
        return buildingService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Update building details")
    public BuildingResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBuildingRequest request) {
        return buildingService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update building status (admin only)")
    public BuildingResponse updateStatus(
            @PathVariable UUID id,
            @RequestParam Building.BuildingStatus status) {
        return buildingService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a building (only if not tokenized)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        buildingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
