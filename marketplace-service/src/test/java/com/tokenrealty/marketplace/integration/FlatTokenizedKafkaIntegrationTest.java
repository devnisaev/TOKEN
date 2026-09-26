package com.tokenrealty.marketplace.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.marketplace.client.TokenIssuanceClient;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.command.FlatTokenizedCommand;
import com.tokenrealty.marketplace.repository.ListingRepository;
import com.tokenrealty.marketplace.service.ListingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Flat tokenized Kafka integration test")
class FlatTokenizedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ListingService listingService;
    @Autowired ListingRepository listingRepository;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TokenIssuanceClient tokenIssuanceClient;

    @Test
    @DisplayName("flat.tokenized creates primary listing")
    void flatTokenized_createsPrimaryListing() throws Exception {
        UUID flatId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(tokenIssuanceClient.getContractByFlatId(eq(flatId)))
                .thenReturn(new TokenIssuanceClient.TokenContractResponse(
                        contractId, flatId, "0xSPV", "0xContract", 1000L,
                        new BigDecimal("45.00"), "ACTIVE"));

        ingestFlatTokenized(flatId, UUID.randomUUID(), contractId.toString(), 1000L, "45.00");

        assertThat(listingRepository.findByFlatIdAndStatus(flatId, Listing.ListingStatus.ACTIVE))
                .isPresent()
                .get()
                .satisfies(listing -> {
                    assertThat(listing.getListingType()).isEqualTo(Listing.ListingType.PRIMARY);
                    assertThat(listing.getStatus()).isEqualTo(Listing.ListingStatus.ACTIVE);
                    assertThat(listing.getTokensTotal()).isEqualTo(1000L);
                    assertThat(listing.getPriceUsd()).isEqualByComparingTo("45.00");
                });
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void flatTokenized_dedupesDuplicateEventId() throws Exception {
        UUID flatId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(tokenIssuanceClient.getContractByFlatId(eq(flatId)))
                .thenReturn(new TokenIssuanceClient.TokenContractResponse(
                        contractId, flatId, "0xSPV", "0xContract", 500L,
                        new BigDecimal("50.00"), "ACTIVE"));

        ingestFlatTokenized(flatId, eventId, contractId.toString(), 500L, "50.00");
        ingestFlatTokenized(flatId, eventId, contractId.toString(), 500L, "50.00");

        assertThat(listingRepository.findAll().stream().filter(l -> l.getFlatId().equals(flatId)).count())
                .isEqualTo(1);
    }

    private void ingestFlatTokenized(
            UUID flatId,
            UUID eventId,
            String contractAddress,
            long totalTokens,
            String tokenPriceUsd) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flatId", flatId.toString());
        payload.put("buildingId", UUID.randomUUID().toString());
        payload.put("contractAddress", contractAddress);
        payload.put("totalTokens", totalTokens);
        payload.put("tokenPriceUsd", tokenPriceUsd);

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                MarketplaceKafkaEventTypes.FLAT_TOKENIZED,
                flatId.toString(),
                payload));
        eventConsumer.consume(message, MarketplaceKafkaEventTypes.FLAT_TOKENIZED,
                "Flat tokenized processing failed",
                event -> listingService.createFromFlatTokenized(FlatTokenizedCommand.from(event)));
    }
}
