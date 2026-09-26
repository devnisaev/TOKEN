package com.tokenrealty.search.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record FlatTokenizedCommand(
        UUID eventId,
        UUID flatId,
        UUID buildingId,
        String contractAddress,
        long totalTokens,
        BigDecimal tokenPriceUsd
) {
    public static FlatTokenizedCommand from(KafkaJsonEvent event) {
        return new FlatTokenizedCommand(
                event.eventId(),
                event.requireUuid("flatId"),
                event.requireUuid("buildingId"),
                event.requireText("contractAddress"),
                event.requireInt("totalTokens"),
                event.requireDecimal("tokenPriceUsd"));
    }
}
