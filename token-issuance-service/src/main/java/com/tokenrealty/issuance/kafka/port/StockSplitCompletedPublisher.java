package com.tokenrealty.issuance.kafka.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface StockSplitCompletedPublisher {

    void publishStockSplitCompleted(StockSplitCompletedEvent event);

    record StockSplitCompletedEvent(
            UUID corporateActionId,
            UUID contractId,
            UUID flatId,
            String period,
            BigDecimal splitRatio,
            Long newTotalSupply,
            BigDecimal newTokenPriceUsd,
            Instant completedAt
    ) {
    }
}
