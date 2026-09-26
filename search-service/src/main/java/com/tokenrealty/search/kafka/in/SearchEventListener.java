package com.tokenrealty.search.kafka.in;

import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.search.kafka.SearchKafkaEventTypes;
import com.tokenrealty.search.kafka.command.BuildingApprovedCommand;
import com.tokenrealty.search.kafka.command.FlatTokenizedCommand;
import com.tokenrealty.search.kafka.command.ListingCreatedCommand;
import com.tokenrealty.search.kafka.command.ValuationUpdatedCommand;
import com.tokenrealty.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SearchEventListener {

    private final KafkaEventConsumer eventConsumer;
    private final SearchIndexService searchIndexService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.listing-created}")
    public void onListingCreated(String message) {
        ingest(message, SearchKafkaEventTypes.LISTING_CREATED,
                event -> searchIndexService.onListingCreated(ListingCreatedCommand.from(event)));
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.flat-tokenized}")
    public void onFlatTokenized(String message) {
        ingest(message, SearchKafkaEventTypes.FLAT_TOKENIZED,
                event -> searchIndexService.onFlatTokenized(FlatTokenizedCommand.from(event)));
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.building-approved}")
    public void onBuildingApproved(String message) {
        ingest(message, SearchKafkaEventTypes.BUILDING_APPROVED,
                event -> searchIndexService.onBuildingApproved(BuildingApprovedCommand.from(event)));
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.valuation-updated}")
    public void onValuationUpdated(String message) {
        ingest(message, SearchKafkaEventTypes.VALUATION_UPDATED,
                event -> searchIndexService.onValuationUpdated(ValuationUpdatedCommand.from(event)));
    }

    private void ingest(String message, String eventType,
                        java.util.function.Consumer<com.tokenrealty.events.kafka.KafkaJsonEvent> handler) {
        eventConsumer.consume(message, eventType, "Search index ingest failed", handler);
    }
}
