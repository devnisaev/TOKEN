package com.tokenrealty.registry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.entity.Valuation;
import com.tokenrealty.registry.kafka.RegistryKafkaEventTypes;
import com.tokenrealty.registry.kafka.command.ValuationUpdatedCommand;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.FlatRepository;
import com.tokenrealty.registry.repository.ValuationRepository;
import com.tokenrealty.registry.service.ValuationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Registry valuation.updated Kafka integration test")
class ValuationUpdatedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ValuationService valuationService;
    @Autowired FlatRepository flatRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired ValuationRepository valuationRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("valuation.updated syncs Valuation row and token price")
    void valuationUpdated_syncsValuationAndTokenPrice() throws Exception {
        UUID flatId = seedTokenizedFlat(1000L);
        UUID buildingId = flatRepository.findById(flatId).orElseThrow().getBuilding().getId();
        UUID valuationRequestId = UUID.randomUUID();

        ingestValuationUpdated(flatId, buildingId, valuationRequestId, UUID.randomUUID());

        Valuation valuation = valuationRepository.findByFlatIdAndIsCurrentTrue(flatId).orElseThrow();
        assertThat(valuation.getValueUsd()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(valuation.getMethod()).isEqualTo(Valuation.ValuationMethod.AUTOMATED);
        assertThat(valuation.getAppraiserName()).isEqualTo("Valuation Service");

        Flat flat = flatRepository.findById(flatId).orElseThrow();
        assertThat(flat.getTokenPriceUsd()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void valuationUpdated_dedupesDuplicateEventId() throws Exception {
        UUID flatId = seedTokenizedFlat(500L);
        UUID buildingId = flatRepository.findById(flatId).orElseThrow().getBuilding().getId();
        UUID eventId = UUID.randomUUID();

        ingestValuationUpdated(flatId, buildingId, UUID.randomUUID(), eventId);
        ingestValuationUpdated(flatId, buildingId, UUID.randomUUID(), eventId);

        assertThat(valuationRepository.findByFlatIdOrderByValuationDateDesc(flatId)).hasSize(1);
    }

    private UUID seedTokenizedFlat(long totalTokens) {
        Building building = buildingRepository.save(Building.builder()
                .name("Valuation Tower")
                .address("2 Test Ave")
                .city("Bishkek")
                .country("KG")
                .postalCode("720001")
                .totalFloors(10)
                .totalFlats(40)
                .constructionYear(2020)
                .status(Building.BuildingStatus.TOKENIZED)
                .build());

        Flat flat = flatRepository.save(Flat.builder()
                .building(building)
                .flatNumber("201")
                .status(Flat.FlatStatus.TOKENIZED)
                .tokenContractAddress("0xValToken")
                .totalTokens(totalTokens)
                .tokenPriceUsd(new BigDecimal("900.00"))
                .build());
        return flat.getId();
    }

    private void ingestValuationUpdated(UUID flatId, UUID buildingId, UUID valuationRequestId,
                                        UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flatId", flatId.toString());
        payload.put("buildingId", buildingId.toString());
        payload.put("valuationRequestId", valuationRequestId.toString());
        payload.put("valueUsd", "1000000.00");
        payload.put("totalTokens", 1000);
        payload.put("navPerTokenUsd", "1000.00000000");
        payload.put("approvedAt", Instant.parse("2025-09-25T12:00:00Z").toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                RegistryKafkaEventTypes.VALUATION_UPDATED,
                "test-trace",
                payload));
        eventConsumer.consume(message, RegistryKafkaEventTypes.VALUATION_UPDATED,
                "Valuation updated processing failed",
                event -> valuationService.syncFromValuationUpdated(ValuationUpdatedCommand.from(event)));
    }
}
