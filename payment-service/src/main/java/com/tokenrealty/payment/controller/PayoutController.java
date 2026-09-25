package com.tokenrealty.payment.controller;

import com.tokenrealty.payment.dto.PaymentDtos.*;
import com.tokenrealty.payment.service.PayoutService;
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
@RequestMapping("/v1/payouts")
@RequiredArgsConstructor
@Tag(name = "Payouts", description = "Dividend and rent payouts")
public class PayoutController {

    private final PayoutService payoutService;

    @GetMapping
    @Operation(summary = "List payouts")
    public Page<PayoutResponse> list(
            @RequestParam(required = false) UUID recipientInvestorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return payoutService.findAll(recipientInvestorId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a payout to a holder wallet")
    public PayoutResponse create(@Valid @RequestBody CreatePayoutRequest request) {
        return payoutService.create(request);
    }
}
