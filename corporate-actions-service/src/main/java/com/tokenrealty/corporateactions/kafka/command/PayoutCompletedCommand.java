package com.tokenrealty.corporateactions.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record PayoutCompletedCommand(
        UUID eventId,
        UUID payoutId,
        UUID contractId,
        BigDecimal amountUsd
) {
    public static PayoutCompletedCommand from(KafkaJsonEvent event) {
        return new PayoutCompletedCommand(
                event.eventId(),
                event.requireUuid("payoutId"),
                event.optionalUuid("contractId"),
                event.payload().hasNonNull("grossAmountUsd")
                        ? event.requireDecimal("grossAmountUsd")
                        : null);
    }
}
