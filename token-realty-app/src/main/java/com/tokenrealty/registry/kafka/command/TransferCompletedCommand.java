package com.tokenrealty.registry.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.util.UUID;

public record TransferCompletedCommand(
        UUID eventId,
        UUID flatId,
        long tokenAmount
) {
    public static TransferCompletedCommand from(KafkaJsonEvent event) {
        return new TransferCompletedCommand(
                event.eventId(),
                event.requireUuid("flatId"),
                event.payload().path("tokenAmount").asLong(0));
    }
}
