package com.tokenrealty.search.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ValuationUpdatedCommand(
        UUID eventId,
        UUID flatId,
        UUID buildingId,
        UUID valuationRequestId,
        BigDecimal valueUsd,
        long totalTokens,
        BigDecimal navPerTokenUsd,
        Instant approvedAt
) {
    public static ValuationUpdatedCommand from(KafkaJsonEvent event) {
        return new ValuationUpdatedCommand(
                event.eventId(),
                event.requireUuid("flatId"),
                event.requireUuid("buildingId"),
                event.requireUuid("valuationRequestId"),
                event.requireDecimal("valueUsd"),
                event.requireInt("totalTokens"),
                event.requireDecimal("navPerTokenUsd"),
                Instant.parse(event.requireText("approvedAt")));
    }
}
