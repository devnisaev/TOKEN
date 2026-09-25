package com.tokenrealty.payment.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.util.UUID;

public record OrderMatchedCommand(
        UUID eventId,
        UUID orderId,
        UUID paymentId
) {
    public static OrderMatchedCommand from(KafkaJsonEvent event) {
        return new OrderMatchedCommand(
                event.eventId(),
                event.requireUuid("orderId"),
                event.optionalUuid("paymentId"));
    }
}
