package com.tokenrealty.rental.controller;

import com.tokenrealty.rental.dto.RentalDtos.IngestOperatorRevenueRequest;
import com.tokenrealty.rental.dto.RentalDtos.OperatorRevenueResponse;
import com.tokenrealty.rental.service.OperatorRevenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/operator-revenue")
@RequiredArgsConstructor
@Tag(name = "Operator Revenue", description = "Operating asset revenue ingestion (Phase 12)")
public class OperatorRevenueController {

    private final OperatorRevenueService operatorRevenueService;

    @GetMapping
    @Operation(summary = "List operator revenue records for a flat")
    public List<OperatorRevenueResponse> listByFlat(@RequestParam UUID flatId) {
        return operatorRevenueService.findByFlat(flatId);
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Ingest operator P&L for an asset unit")
    public OperatorRevenueResponse ingest(@Valid @RequestBody IngestOperatorRevenueRequest request) {
        return operatorRevenueService.ingest(request, "direct");
    }

    @PostMapping("/feeds/{provider}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Ingest operator revenue from integration feed")
    public OperatorRevenueResponse ingestFeed(
            @PathVariable String provider,
            @Valid @RequestBody IngestOperatorRevenueRequest request) {
        return operatorRevenueService.ingest(request, provider);
    }
}
