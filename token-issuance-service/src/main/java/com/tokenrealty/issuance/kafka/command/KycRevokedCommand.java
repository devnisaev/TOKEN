package com.tokenrealty.issuance.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.util.UUID;

public record KycRevokedCommand(
        UUID investorId,
        String walletAddress,
        String reason
) {

    public static KycRevokedCommand from(KafkaJsonEvent event) {
        return new KycRevokedCommand(
                event.requireUuid("investorId"),
                event.requireText("walletAddress"),
                event.requireText("reason"));
    }
}
