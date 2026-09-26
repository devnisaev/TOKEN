package com.tokenrealty.valuation.kafka.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface ValuationEventPublisher {

    record ValuationUpdatedEvent(
            UUID flatId,
            UUID buildingId,
            UUID valuationRequestId,
            BigDecimal valueUsd,
            long totalTokens,
            BigDecimal navPerTokenUsd,
            Instant approvedAt
    ) {
    }

    record ValuationApprovedEvent(
            UUID flatId,
            UUID buildingId,
            UUID valuationRequestId,
            BigDecimal valueUsd,
            long totalTokens,
            BigDecimal navPerTokenUsd,
            Instant approvedAt,
            UUID reviewedBy
    ) {
    }

    void publishValuationUpdated(ValuationUpdatedEvent event);

    void publishValuationApproved(ValuationApprovedEvent event);
}
