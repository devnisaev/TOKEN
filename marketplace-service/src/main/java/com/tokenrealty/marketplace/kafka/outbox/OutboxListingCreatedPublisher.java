package com.tokenrealty.marketplace.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.port.ListingCreatedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxListingCreatedPublisher implements ListingCreatedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.listing-created:" + MarketplaceKafkaEventTypes.LISTING_CREATED + "}")
    private String listingCreatedTopic;

    @Override
    public void publishListingCreated(ListingCreatedEvent event) {
        OutboxPayload.start()
                .put("listingId", event.listingId())
                .put("flatId", event.flatId())
                .put("listingType", event.listingType())
                .put("priceUsd", event.priceUsd())
                .put("tokensAvailable", event.tokensAvailable())
                .enqueue(outboxWriter, listingCreatedTopic, event.listingId());
    }
}
