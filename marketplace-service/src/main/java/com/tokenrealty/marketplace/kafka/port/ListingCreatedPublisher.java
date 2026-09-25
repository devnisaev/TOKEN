package com.tokenrealty.marketplace.kafka.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface ListingCreatedPublisher {

    void publishListingCreated(ListingCreatedEvent event);

    record ListingCreatedEvent(
            UUID listingId,
            UUID flatId,
            String listingType,
            BigDecimal priceUsd,
            long tokensAvailable
    ) {
    }
}
