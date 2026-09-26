package com.tokenrealty.valuation.controller;

import com.tokenrealty.security.TokenPrincipal;
import com.tokenrealty.valuation.dto.ValuationDtos.NavSnapshotResponse;
import com.tokenrealty.valuation.dto.ValuationDtos.RejectValuationRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.SubmitValuationRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.ValuationRequestResponse;
import com.tokenrealty.valuation.service.ValuationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/valuations")
@RequiredArgsConstructor
@Tag(name = "Valuations", description = "Appraisal workflow and NAV snapshots")
public class ValuationController {

    private final ValuationService valuationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a valuation request for admin review")
    public ValuationRequestResponse submit(
            @Valid @RequestBody SubmitValuationRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {
        return valuationService.submit(request, principal.userId());
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending valuation and publish NAV snapshot")
    public ValuationRequestResponse approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal TokenPrincipal principal) {
        return valuationService.approve(id, principal.userId());
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending valuation request")
    public ValuationRequestResponse reject(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectValuationRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {
        return valuationService.reject(id, principal.userId(), request);
    }

    @GetMapping("/building/{buildingId}")
    @Operation(summary = "List valuation requests for a building")
    public List<ValuationRequestResponse> listByBuilding(@PathVariable UUID buildingId) {
        return valuationService.findByBuilding(buildingId);
    }

    @GetMapping("/flat/{flatId}/nav")
    @Operation(summary = "Get latest approved NAV snapshot for a flat")
    public NavSnapshotResponse getLatestNav(@PathVariable UUID flatId) {
        return valuationService.getLatestNav(flatId);
    }
}
