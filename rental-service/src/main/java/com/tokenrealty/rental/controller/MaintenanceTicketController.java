package com.tokenrealty.rental.controller;

import com.tokenrealty.rental.dto.RentalDtos.*;
import com.tokenrealty.rental.service.MaintenanceTicketService;
import com.tokenrealty.security.TokenPrincipal;
import com.tokenrealty.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/maintenance-tickets")
@RequiredArgsConstructor
public class MaintenanceTicketController {

    private final MaintenanceTicketService maintenanceTicketService;

    @GetMapping
    public List<MaintenanceTicketResponse> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID leaseId) {
        return maintenanceTicketService.list(tenantId, leaseId);
    }

    @GetMapping("/{id}")
    public MaintenanceTicketResponse findById(@PathVariable UUID id) {
        return maintenanceTicketService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_MANAGER', 'TENANT')")
    public MaintenanceTicketResponse create(
            @Valid @RequestBody CreateMaintenanceTicketRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {
        UUID callerTenantId = principal.role() == UserRole.TENANT ? principal.userId() : null;
        return maintenanceTicketService.create(request, callerTenantId);
    }
}
