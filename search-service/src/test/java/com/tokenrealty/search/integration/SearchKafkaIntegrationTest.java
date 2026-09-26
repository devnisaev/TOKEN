package com.tokenrealty.search.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.search.kafka.SearchKafkaEventTypes;
import com.tokenrealty.search.kafka.command.BuildingApprovedCommand;
import com.tokenrealty.search.kafka.command.FlatTokenizedCommand;
import com.tokenrealty.search.kafka.command.ListingCreatedCommand;
import com.tokenrealty.search.kafka.command.ValuationUpdatedCommand;
import com.tokenrealty.search.repository.BuildingIndexRepository;
import com.tokenrealty.search.repository.ListingIndexRepository;
import com.tokenrealty.search.service.SearchIndexService;
import com.tokenrealty.search.service.SearchQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SearchKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired SearchIndexService searchIndexService;
    @Autowired SearchQueryService searchQueryService;
    @Autowired ListingIndexRepository listingIndexRepository;
    @Autowired BuildingIndexRepository buildingIndexRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        listingIndexRepository.deleteAll();
        buildingIndexRepository.deleteAll();
    }

    @Test
    @DisplayName("listing.created indexes searchable listing")
    void listingCreated_indexed() throws Exception {
        UUID listingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();
        publishListingCreated(listingId, flatId, UUID.randomUUID());

        assertThat(listingIndexRepository.count()).isEqualTo(1);
        var results = searchQueryService.searchListings(flatId.toString(), null, null, null, Pageable.unpaged());
        assertThat(results.getTotalElements()).isEqualTo(1);
        assertThat(results.getContent().getFirst().getListingId()).isEqualTo(listingId);
    }

    @Test
    @DisplayName("building.approved and flat.tokenized update building index")
    void buildingApprovedAndFlatTokenized_indexBuilding() throws Exception {
        UUID buildingId = UUID.randomUUID();
        publishBuildingApproved(buildingId, UUID.randomUUID());
        publishFlatTokenized(UUID.randomUUID(), buildingId, UUID.randomUUID());

        assertThat(buildingIndexRepository.count()).isEqualTo(1);
        var building = buildingIndexRepository.findByBuildingId(buildingId).orElseThrow();
        assertThat(building.getFlatCount()).isEqualTo(1);
        assertThat(building.getApprovedAt()).isNotNull();
        assertThat(building.getLatestTokenPriceUsd()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("valuation.updated enriches listing and building NAV")
    void valuationUpdated_enrichesNav() throws Exception {
        UUID listingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();
        publishListingCreated(listingId, flatId, UUID.randomUUID());
        publishValuationUpdated(flatId, buildingId, UUID.randomUUID());

        var listing = listingIndexRepository.findByListingId(listingId).orElseThrow();
        assertThat(listing.getNavPerTokenUsd()).isEqualByComparingTo(new BigDecimal("105.50"));
        assertThat(listing.getBuildingId()).isEqualTo(buildingId);

        var building = buildingIndexRepository.findByBuildingId(buildingId).orElseThrow();
        assertThat(building.getLatestNavPerTokenUsd()).isEqualByComparingTo(new BigDecimal("105.50"));
    }

    @Test
    @DisplayName("duplicate eventId is deduped")
    void duplicateEventId_deduped() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        publishListingCreated(listingId, UUID.randomUUID(), eventId);
        publishListingCreated(listingId, UUID.randomUUID(), eventId);

        assertThat(listingIndexRepository.count()).isEqualTo(1);
    }

    private void publishListingCreated(UUID listingId, UUID flatId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listingId", listingId.toString());
        payload.put("flatId", flatId.toString());
        payload.put("listingType", "PRIMARY");
        payload.put("priceUsd", "100.00");
        payload.put("tokensAvailable", 5000);
        publish(SearchKafkaEventTypes.LISTING_CREATED, eventId, payload,
                event -> searchIndexService.onListingCreated(ListingCreatedCommand.from(event)));
    }

    private void publishBuildingApproved(UUID buildingId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId.toString());
        payload.put("approvedAt", "2025-09-25T16:00:00Z");
        payload.put("approvedBy", "admin@tokenrealty.com");
        publish(SearchKafkaEventTypes.BUILDING_APPROVED, eventId, payload,
                event -> searchIndexService.onBuildingApproved(BuildingApprovedCommand.from(event)));
    }

    private void publishFlatTokenized(UUID flatId, UUID buildingId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flatId", flatId.toString());
        payload.put("buildingId", buildingId.toString());
        payload.put("contractAddress", "0xabc");
        payload.put("totalTokens", 10000);
        payload.put("tokenPriceUsd", "100.00");
        publish(SearchKafkaEventTypes.FLAT_TOKENIZED, eventId, payload,
                event -> searchIndexService.onFlatTokenized(FlatTokenizedCommand.from(event)));
    }

    private void publishValuationUpdated(UUID flatId, UUID buildingId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flatId", flatId.toString());
        payload.put("buildingId", buildingId.toString());
        payload.put("valuationRequestId", UUID.randomUUID().toString());
        payload.put("valueUsd", "1055000.00");
        payload.put("totalTokens", 10000);
        payload.put("navPerTokenUsd", "105.50");
        payload.put("approvedAt", "2025-09-26T10:00:00Z");
        publish(SearchKafkaEventTypes.VALUATION_UPDATED, eventId, payload,
                event -> searchIndexService.onValuationUpdated(ValuationUpdatedCommand.from(event)));
    }

    private void publish(String eventType, UUID eventId, Map<String, Object> payload,
                         java.util.function.Consumer<com.tokenrealty.events.kafka.KafkaJsonEvent> handler)
            throws Exception {
        EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                eventId, eventType, Instant.parse("2025-09-25T16:00:00Z"), null, payload);
        eventConsumer.consume(objectMapper.writeValueAsString(envelope), eventType, "Search test failed", handler);
    }
}
