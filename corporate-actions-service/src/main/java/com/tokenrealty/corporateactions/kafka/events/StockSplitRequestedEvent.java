package com.tokenrealty.corporateactions.kafka.events;

import java.math.BigDecimal;
import java.util.UUID;

public record StockSplitRequestedEvent(
        UUID corporateActionId,
        UUID flatId,
        UUID contractId,
        String period,
        BigDecimal splitRatio
) {
}
