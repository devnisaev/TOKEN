package com.tokenrealty.registry.kafka.port;

import java.time.Instant;
import java.util.UUID;

public interface BuildingApprovedPublisher {

    void publishBuildingApproved(BuildingApprovedEvent event);

    record BuildingApprovedEvent(UUID buildingId, Instant approvedAt, String approvedBy) {
    }
}
