package com.tokenrealty.registry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.entity.Flat.FlatStatus;
import com.tokenrealty.registry.kafka.RegistryKafkaEventTypes;
import com.tokenrealty.registry.kafka.command.TransferCompletedCommand;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.FlatRepository;
import com.tokenrealty.registry.service.FlatService;
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
@DisplayName("Registry transfer.completed Kafka integration test")
class TransferCompletedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired FlatService flatService;
    @Autowired FlatRepository flatRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("transfer.completed marks flat FULLY_SOLD when token amount covers supply")
    void transferCompleted_marksFlatFullySold() throws Exception {
        UUID flatId = seedTokenizedFlat(100L);

        ingestTransferCompleted(flatId, 100L, UUID.randomUUID());

        Flat flat = flatRepository.findById(flatId).orElseThrow();
        assertThat(flat.getStatus()).isEqualTo(FlatStatus.FULLY_SOLD);
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void transferCompleted_dedupesDuplicateEventId() throws Exception {
        UUID flatId = seedTokenizedFlat(50L);
        UUID eventId = UUID.randomUUID();

        ingestTransferCompleted(flatId, 50L, eventId);
        ingestTransferCompleted(flatId, 50L, eventId);

        assertThat(flatRepository.findById(flatId).orElseThrow().getStatus())
                .isEqualTo(FlatStatus.FULLY_SOLD);
    }

    private UUID seedTokenizedFlat(long totalTokens) {
        Building building = buildingRepository.save(Building.builder()
                .name("Test Tower")
                .address("1 Test Ave")
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
                .flatNumber("101")
                .status(FlatStatus.TOKENIZED)
                .tokenContractAddress("0xDemoToken")
                .totalTokens(totalTokens)
                .tokenPriceUsd(new BigDecimal("45.00"))
                .build());
        return flat.getId();
    }

    private void ingestTransferCompleted(UUID flatId, long tokenAmount, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferId", UUID.randomUUID().toString());
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("flatId", flatId.toString());
        payload.put("orderId", UUID.randomUUID().toString());
        payload.put("tradeId", UUID.randomUUID().toString());
        payload.put("paymentId", UUID.randomUUID().toString());
        payload.put("tokenAmount", tokenAmount);
        payload.put("txHash", "0xTransferCompleted");
        payload.put("completedAt", Instant.now().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                RegistryKafkaEventTypes.TRANSFER_COMPLETED,
                "test-trace",
                payload));
        eventConsumer.consume(message, RegistryKafkaEventTypes.TRANSFER_COMPLETED,
                "Transfer completed processing failed",
                event -> flatService.syncFromTransferCompleted(TransferCompletedCommand.from(event)));
    }
}
