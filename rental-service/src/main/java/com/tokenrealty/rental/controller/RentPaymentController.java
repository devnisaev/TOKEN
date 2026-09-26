package com.tokenrealty.rental.controller;

import com.tokenrealty.rental.dto.RentalDtos.*;
import com.tokenrealty.rental.service.RentPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/rent-payments")
@RequiredArgsConstructor
public class RentPaymentController {

    private final RentPaymentService rentPaymentService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_MANAGER', 'COMPLIANCE')")
    public RentSummaryResponse summary(
            @RequestParam UUID flatId,
            @RequestParam String period) {
        return rentPaymentService.summarize(flatId, period);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_MANAGER')")
    public RentPaymentResponse record(@Valid @RequestBody RecordRentPaymentRequest request) {
        return rentPaymentService.record(request);
    }
}
