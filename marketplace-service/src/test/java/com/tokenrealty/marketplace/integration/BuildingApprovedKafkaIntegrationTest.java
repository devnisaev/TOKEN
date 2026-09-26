package com.tokenrealty.marketplace.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.command.BuildingApprovedCommand;
import com.tokenrealty.marketplace.repository.ApprovedBuildingRepository;
import com.tokenrealty.marketplace.service.ListingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Building approved Kafka integration test")
class BuildingApprovedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ListingService listingService;
    @Autowired ApprovedBuildingRepository approvedBuildingRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("building.approved records approved building")
    void buildingApproved_recordsApprovedBuilding() throws Exception {
        UUID buildingId = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2025-09-25T16:00:00Z");

        ingestBuildingApproved(buildingId, UUID.randomUUID(), approvedAt, "spv-registration");

        assertThat(approvedBuildingRepository.findById(buildingId))
                .isPresent()
                .get()
                .satisfies(building -> {
                    assertThat(building.getBuildingId()).isEqualTo(buildingId);
                    assertThat(building.getApprovedAt()).isEqualTo(approvedAt);
                    assertThat(building.getApprovedBy()).isEqualTo("spv-registration");
                });
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void buildingApproved_dedupesDuplicateEventId() throws Exception {
        UUID buildingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant approvedAt = Instant.now();

        ingestBuildingApproved(buildingId, eventId, approvedAt, "admin");
        ingestBuildingApproved(buildingId, eventId, approvedAt, "admin");

        assertThat(approvedBuildingRepository.findAll().stream()
                .filter(b -> b.getBuildingId().equals(buildingId))
                .count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("duplicate buildingId is idempotent")
    void buildingApproved_idempotentForSameBuilding() throws Exception {
        UUID buildingId = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2025-09-25T16:00:00Z");

        ingestBuildingApproved(buildingId, UUID.randomUUID(), approvedAt, "spv-registration");
        ingestBuildingApproved(buildingId, UUID.randomUUID(), approvedAt, "spv-registration");

        assertThat(approvedBuildingRepository.findAll().stream()
                .filter(b -> b.getBuildingId().equals(buildingId))
                .count())
                .isEqualTo(1);
    }

    private void ingestBuildingApproved(
            UUID buildingId,
            UUID eventId,
            Instant approvedAt,
            String approvedBy) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId.toString());
        payload.put("approvedAt", approvedAt.toString());
        payload.put("approvedBy", approvedBy);

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                MarketplaceKafkaEventTypes.BUILDING_APPROVED,
                buildingId.toString(),
                payload));
        eventConsumer.consume(message, MarketplaceKafkaEventTypes.BUILDING_APPROVED,
                "Building approved processing failed",
                event -> listingService.recordBuildingApproved(BuildingApprovedCommand.from(event)));
    }
}
