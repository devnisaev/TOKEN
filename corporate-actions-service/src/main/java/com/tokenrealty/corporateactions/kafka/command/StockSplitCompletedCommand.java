package com.tokenrealty.corporateactions.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.util.UUID;

public record StockSplitCompletedCommand(
        UUID corporateActionId,
        UUID contractId,
        UUID flatId,
        String period
) {
    public static StockSplitCompletedCommand from(KafkaJsonEvent event) {
        return new StockSplitCompletedCommand(
                event.requireUuid("corporateActionId"),
                event.optionalUuid("contractId"),
                event.requireUuid("flatId"),
                event.requireText("period"));
    }
}
