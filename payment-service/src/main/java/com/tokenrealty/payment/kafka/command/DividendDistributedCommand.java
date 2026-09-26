package com.tokenrealty.payment.kafka.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DividendDistributedCommand(
        UUID eventId,
        UUID contractId,
        UUID flatId,
        String period,
        List<HolderPayout> holderPayouts
) {
    public record HolderPayout(UUID dividendPaymentId, UUID investorId, String walletAddress, BigDecimal amount) {
    }

    public static DividendDistributedCommand from(KafkaJsonEvent event) {
        List<HolderPayout> payouts = new ArrayList<>();
        JsonNode holders = event.payload().get("holderPayouts");
        if (holders != null && holders.isArray()) {
            for (JsonNode node : holders) {
                payouts.add(new HolderPayout(
                        UUID.fromString(node.get("dividendPaymentId").asText()),
                        UUID.fromString(node.get("investorId").asText()),
                        node.get("walletAddress").asText(),
                        new BigDecimal(node.get("amount").asText())));
            }
        }
        return new DividendDistributedCommand(
                event.eventId(),
                event.requireUuid("contractId"),
                event.optionalUuid("flatId"),
                event.requireText("period"),
                List.copyOf(payouts));
    }
}
