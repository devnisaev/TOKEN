package com.tokenrealty.payment.kafka.events;

import com.tokenrealty.payment.entity.PaymentCurrency;

import java.util.UUID;

public record RentCollectedEvent(
        UUID payoutId,
        UUID leaseId,
        UUID flatId,
        UUID tenantId,
        String period,
        Amount amount,
        String txHash
) {
    public record Amount(String value, PaymentCurrency currency) {
    }
}
