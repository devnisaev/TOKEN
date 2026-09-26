package com.tokenrealty.issuance.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record DividendDistributionRequestedCommand(
        UUID corporateActionId,
        UUID flatId,
        UUID contractId,
        String period,
        BigDecimal grossAmountUsd
) {
    public static DividendDistributionRequestedCommand from(KafkaJsonEvent event) {
        BigDecimal amount = event.payload().has("grossAmountUsd")
                ? event.requireDecimal("grossAmountUsd")
                : new BigDecimal(event.payload().get("amount").get("value").asText());
        return new DividendDistributionRequestedCommand(
                event.requireUuid("corporateActionId"),
                event.requireUuid("flatId"),
                event.optionalUuid("contractId"),
                event.requireText("period"),
                amount);
    }
}
