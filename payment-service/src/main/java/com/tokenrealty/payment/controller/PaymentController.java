package com.tokenrealty.payment.controller;

import com.tokenrealty.payment.dto.PaymentDtos.*;
import com.tokenrealty.payment.service.PaymentService;
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
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Crypto payments and escrow")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "List payments")
    public Page<PaymentResponse> list(
            @RequestParam(required = false) UUID payerId,
            @RequestParam(required = false) UUID orderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return paymentService.findAll(payerId, orderId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by id")
    public PaymentResponse getById(@PathVariable UUID id) {
        return paymentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Initiate a crypto payment with escrow")
    public PaymentResponse initiate(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {
        return paymentService.initiate(request, idempotencyKey);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Confirm on-chain payment (webhook/callback)")
    public PaymentResponse confirm(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return paymentService.confirm(id, request);
    }

    @PatchMapping("/{id}/release")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Release escrow after token transfer")
    public PaymentResponse release(@PathVariable UUID id) {
        return paymentService.releaseEscrow(id);
    }

    @PatchMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund escrow to payer")
    public PaymentResponse refund(@PathVariable UUID id) {
        return paymentService.refund(id);
    }
}
