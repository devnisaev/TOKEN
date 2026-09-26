package com.tokenrealty.rental.controller;

import com.tokenrealty.rental.dto.RentalDtos.*;
import com.tokenrealty.rental.service.RentPaymentService;
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
@RequestMapping("/v1/rent-payments")
@RequiredArgsConstructor
public class RentPaymentController {

    private final RentPaymentService rentPaymentService;

    @GetMapping
    public List<RentPaymentResponse> list(@RequestParam UUID leaseId) {
        return rentPaymentService.listByLeaseId(leaseId);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_MANAGER', 'COMPLIANCE')")
    public RentSummaryResponse summary(
            @RequestParam UUID flatId,
            @RequestParam String period) {
        return rentPaymentService.summarize(flatId, period);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_MANAGER', 'TENANT')")
    public RentPaymentResponse record(
            @Valid @RequestBody RecordRentPaymentRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {
        boolean tenantCaller = principal.role() == UserRole.TENANT;
        return rentPaymentService.record(request, principal.userId(), tenantCaller);
    }
}
