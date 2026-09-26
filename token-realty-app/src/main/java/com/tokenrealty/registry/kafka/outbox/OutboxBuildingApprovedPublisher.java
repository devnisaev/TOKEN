package com.tokenrealty.registry.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.registry.kafka.RegistryKafkaEventTypes;
import com.tokenrealty.registry.kafka.port.BuildingApprovedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxBuildingApprovedPublisher implements BuildingApprovedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.building-approved:" + RegistryKafkaEventTypes.BUILDING_APPROVED + "}")
    private String buildingApprovedTopic;

    @Override
    public void publishBuildingApproved(BuildingApprovedEvent event) {
        OutboxPayload.start()
                .put("buildingId", event.buildingId())
                .put("approvedAt", event.approvedAt().toString())
                .put("approvedBy", event.approvedBy())
                .enqueue(outboxWriter, buildingApprovedTopic, event.buildingId().toString());
    }
}
