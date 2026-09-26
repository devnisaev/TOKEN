package com.tokenrealty.issuance.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.time.Instant;
import java.util.UUID;

public record KycApprovedCommand(
        UUID investorId,
        String walletAddress,
        String countryCode,
        Instant kycExpiresAt
) {

    public static KycApprovedCommand from(KafkaJsonEvent event) {
        return new KycApprovedCommand(
                event.requireUuid("investorId"),
                event.requireText("walletAddress"),
                event.optionalText("countryCode"),
                Instant.parse(event.requireText("kycExpiresAt")));
    }
}
