package com.tokenrealty.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PaymentWebhookPayload(
        @NotNull UUID paymentId,
        @NotBlank @Size(max = 66) String txHash
) {
}
