package com.tokenrealty.reporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.reporting.kafka.ReportingKafkaEventTypes;
import com.tokenrealty.reporting.repository.BuildingApprovedRecordRepository;
import com.tokenrealty.reporting.service.ReportingProjectionService;
import org.junit.jupiter.api.BeforeEach;
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
class BuildingApprovedReportingIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ReportingProjectionService projectionService;
    @Autowired BuildingApprovedRecordRepository buildingApprovedRecordRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanProjections() {
        buildingApprovedRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("building.approved creates BuildingApprovedRecord projection")
    void buildingApproved_createsProjection() throws Exception {
        UUID buildingId = UUID.randomUUID();
        UUID approvedBy = UUID.randomUUID();
        ingest(buildingId, approvedBy, UUID.randomUUID());

        var record = buildingApprovedRecordRepository.findAll().getFirst();
        assertThat(record.getBuildingId()).isEqualTo(buildingId);
        assertThat(record.getApprovedBy()).isEqualTo(approvedBy);
        assertThat(record.getApprovedAt()).isEqualTo(Instant.parse("2025-09-25T12:00:00Z"));
    }

    private void ingest(UUID buildingId, UUID approvedBy, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId.toString());
        payload.put("approvedBy", approvedBy.toString());
        payload.put("approvedAt", "2025-09-25T12:00:00Z");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                ReportingKafkaEventTypes.BUILDING_APPROVED,
                "test-trace",
                payload));
        eventConsumer.consume(message, ReportingKafkaEventTypes.BUILDING_APPROVED,
                "Building approved projection failed",
                projectionService::onBuildingApproved);
    }
}
