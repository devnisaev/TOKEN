package com.tokenrealty.corporateactions.kafka.events;

import java.math.BigDecimal;
import java.util.UUID;

public record DividendDistributionRequestedEvent(
        UUID corporateActionId,
        UUID flatId,
        UUID contractId,
        String period,
        BigDecimal grossAmountUsd
) {
}
