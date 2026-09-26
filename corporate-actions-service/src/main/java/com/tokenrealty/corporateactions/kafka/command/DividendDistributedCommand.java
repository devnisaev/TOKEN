package com.tokenrealty.corporateactions.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.util.UUID;

public record DividendDistributedCommand(
        UUID eventId,
        UUID contractId,
        UUID flatId,
        String period
) {
    public static DividendDistributedCommand from(KafkaJsonEvent event) {
        return new DividendDistributedCommand(
                event.eventId(),
                event.optionalUuid("contractId"),
                event.requireUuid("flatId"),
                event.requireText("period"));
    }
}
