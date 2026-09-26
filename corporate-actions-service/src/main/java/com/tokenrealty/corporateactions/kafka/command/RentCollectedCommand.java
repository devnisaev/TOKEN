package com.tokenrealty.corporateactions.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record RentCollectedCommand(
        UUID eventId,
        UUID leaseId,
        UUID flatId,
        UUID tenantId,
        String period,
        BigDecimal grossAmountUsd,
        UUID contractId
) {
    public static RentCollectedCommand from(KafkaJsonEvent event) {
        return new RentCollectedCommand(
                event.eventId(),
                event.optionalUuid("leaseId"),
                event.requireUuid("flatId"),
                event.requireUuid("tenantId"),
                event.requireText("period"),
                new BigDecimal(event.payload().get("amount").get("value").asText()),
                event.optionalUuid("contractId"));
    }
}
