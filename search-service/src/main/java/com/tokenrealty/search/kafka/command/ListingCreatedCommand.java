package com.tokenrealty.search.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingCreatedCommand(
        UUID eventId,
        UUID listingId,
        UUID flatId,
        String listingType,
        BigDecimal priceUsd,
        long tokensAvailable
) {
    public static ListingCreatedCommand from(KafkaJsonEvent event) {
        return new ListingCreatedCommand(
                event.eventId(),
                event.requireUuid("listingId"),
                event.requireUuid("flatId"),
                event.requireText("listingType"),
                event.requireDecimal("priceUsd"),
                event.requireInt("tokensAvailable"));
    }
}
