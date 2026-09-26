package com.tokenrealty.rental.controller;

import com.tokenrealty.rental.dto.RentalDtos.*;
import com.tokenrealty.rental.service.LeaseService;
import com.tokenrealty.web.exception.ValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/leases")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leaseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_MANAGER')")
    public LeaseResponse create(@Valid @RequestBody CreateLeaseRequest request) {
        return leaseService.create(request);
    }

    @GetMapping
    public List<LeaseResponse> list(@RequestParam(required = false) UUID tenantId) {
        if (tenantId == null) {
            throw new ValidationException("tenantId query parameter is required");
        }
        return leaseService.listByTenantId(tenantId);
    }

    @GetMapping("/{id}")
    public LeaseResponse findById(@PathVariable UUID id) {
        return leaseService.findById(id);
    }
}
