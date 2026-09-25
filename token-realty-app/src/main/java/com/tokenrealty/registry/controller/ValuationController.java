package com.tokenrealty.registry.controller;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.service.ValuationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/flats/{flatId}/valuations")
@Slf4j
@Tag(name = "Valuations", description = "Flat valuation history")
public class ValuationController {

    private final ValuationService valuationService;

    public ValuationController(ValuationService valuationService) {
        this.valuationService = valuationService;
    }

    @GetMapping
    @Operation(summary = "Get full valuation history for a flat")
    public List<ValuationResponse> listByFlat(@PathVariable UUID flatId) {
        return valuationService.findByFlat(flatId);
    }

    @GetMapping("/current")
    @Operation(summary = "Get the current (latest) valuation for a flat")
    public ValuationResponse getCurrent(@PathVariable UUID flatId) {
        return valuationService.findCurrentByFlat(flatId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific valuation by id")
    public ValuationResponse getById(@PathVariable UUID flatId, @PathVariable UUID id) {
        return valuationService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('APPRAISER')")
    @Operation(summary = "Add a new valuation for a flat")
    public ValuationResponse create(
            @PathVariable UUID flatId,
            @Valid @RequestBody CreateValuationRequest request) {
        return valuationService.create(flatId, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a valuation (non-current only)")
    public ResponseEntity<Void> delete(@PathVariable UUID flatId, @PathVariable UUID id) {
        valuationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}