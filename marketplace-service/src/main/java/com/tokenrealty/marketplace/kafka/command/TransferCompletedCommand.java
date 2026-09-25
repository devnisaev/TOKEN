package com.tokenrealty.marketplace.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.util.UUID;

public record TransferCompletedCommand(
        UUID eventId,
        UUID transferId,
        UUID contractId,
        UUID flatId,
        UUID orderId,
        UUID tradeId,
        UUID paymentId,
        String txHash
) {
    public static TransferCompletedCommand from(KafkaJsonEvent event) {
        return new TransferCompletedCommand(
                event.eventId(),
                event.requireUuid("transferId"),
                event.requireUuid("contractId"),
                event.requireUuid("flatId"),
                event.requireUuid("orderId"),
                event.optionalUuid("tradeId"),
                event.optionalUuid("paymentId"),
                event.optionalText("txHash"));
    }
}
