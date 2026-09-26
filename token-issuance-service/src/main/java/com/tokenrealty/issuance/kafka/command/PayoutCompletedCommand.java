package com.tokenrealty.issuance.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.time.Instant;
import java.util.UUID;

public record PayoutCompletedCommand(
        UUID payoutId,
        UUID dividendPaymentId,
        String txHash,
        Instant completedAt
) {

    public static PayoutCompletedCommand from(KafkaJsonEvent event) {
        return new PayoutCompletedCommand(
                event.requireUuid("payoutId"),
                event.requireUuid("dividendPaymentId"),
                event.requireText("txHash"),
                Instant.parse(event.requireText("completedAt")));
    }
}
