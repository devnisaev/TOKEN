package com.tokenrealty.payment.kafka.events;

import com.tokenrealty.payment.entity.PaymentCurrency;

import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmedEvent(
        UUID paymentId,
        UUID orderId,
        UUID payerId,
        Amount amount,
        String txHash,
        Instant confirmedAt
) {
    public record Amount(String value, PaymentCurrency currency) {
    }
}
