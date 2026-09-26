package com.tokenrealty.search.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.time.Instant;
import java.util.UUID;

public record BuildingApprovedCommand(
        UUID eventId,
        UUID buildingId,
        Instant approvedAt,
        String approvedBy
) {
    public static BuildingApprovedCommand from(KafkaJsonEvent event) {
        return new BuildingApprovedCommand(
                event.eventId(),
                event.requireUuid("buildingId"),
                Instant.parse(event.requireText("approvedAt")),
                event.requireText("approvedBy"));
    }
}
