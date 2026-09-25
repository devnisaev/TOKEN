package com.tokenrealty.marketplace.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.util.UUID;

public record PaymentConfirmedCommand(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        UUID payerId,
        String txHash
) {
    public static PaymentConfirmedCommand from(KafkaJsonEvent event) {
        return new PaymentConfirmedCommand(
                event.eventId(),
                event.requireUuid("paymentId"),
                event.requireUuid("orderId"),
                event.requireUuid("payerId"),
                event.requireText("txHash"));
    }
}
