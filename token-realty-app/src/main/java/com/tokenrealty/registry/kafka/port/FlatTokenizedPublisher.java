package com.tokenrealty.registry.kafka.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface FlatTokenizedPublisher {

    void publishFlatTokenized(FlatTokenizedEvent event);

    record FlatTokenizedEvent(
            UUID flatId,
            UUID buildingId,
            String contractAddress,
            long totalTokens,
            BigDecimal tokenPriceUsd
    ) {
    }
}
