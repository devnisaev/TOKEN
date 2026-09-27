package com.tokenrealty.issuance.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record StockSplitRequestedCommand(
        UUID corporateActionId,
        UUID flatId,
        UUID contractId,
        String period,
        BigDecimal splitRatio
) {
    public static StockSplitRequestedCommand from(KafkaJsonEvent event) {
        return new StockSplitRequestedCommand(
                event.requireUuid("corporateActionId"),
                event.requireUuid("flatId"),
                event.optionalUuid("contractId"),
                event.requireText("period"),
                event.requireDecimal("splitRatio"));
    }
}
