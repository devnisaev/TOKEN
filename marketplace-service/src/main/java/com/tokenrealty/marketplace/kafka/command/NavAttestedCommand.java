package com.tokenrealty.marketplace.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NavAttestedCommand(
        UUID eventId,
        UUID flatId,
        UUID buildingId,
        UUID navSnapshotId,
        BigDecimal navPerTokenUsd,
        Instant attestedAt
) {
    public static NavAttestedCommand from(KafkaJsonEvent event) {
        return new NavAttestedCommand(
                event.eventId(),
                event.requireUuid("flatId"),
                event.requireUuid("buildingId"),
                event.requireUuid("navSnapshotId"),
                event.requireDecimal("navPerTokenUsd"),
                Instant.parse(event.requireText("attestedAt")));
    }
}
