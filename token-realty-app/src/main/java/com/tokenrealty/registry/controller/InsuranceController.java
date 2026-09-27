package com.tokenrealty.registry.controller;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.service.InsuranceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    @GetMapping("/expiring")
    public List<InsuranceExpiryAlertItem> listExpiring(
            @RequestParam(defaultValue = "30") int withinDays) {
        return insuranceService.findExpiringWithinDays(withinDays);
    }

    @GetMapping("/by-building/{buildingId}")
    public List<InsurancePolicyResponse> listByBuilding(@PathVariable UUID buildingId) {
        return insuranceService.findByBuilding(buildingId);
    }

    @GetMapping("/{id}")
    public InsurancePolicyResponse getById(@PathVariable UUID id) {
        return insuranceService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public InsurancePolicyResponse create(@Valid @RequestBody CreateInsurancePolicyRequest request) {
        return insuranceService.create(request);
    }
}
