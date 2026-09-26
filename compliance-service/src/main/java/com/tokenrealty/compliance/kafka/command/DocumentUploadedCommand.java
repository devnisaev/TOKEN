package com.tokenrealty.compliance.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.time.Instant;
import java.util.UUID;

public record DocumentUploadedCommand(
        UUID documentId,
        UUID buildingId,
        UUID flatId,
        String documentType,
        String ipfsCid,
        Instant uploadedAt
) {

    public static DocumentUploadedCommand from(KafkaJsonEvent event) {
        return new DocumentUploadedCommand(
                event.requireUuid("documentId"),
                event.optionalUuid("buildingId"),
                event.optionalUuid("flatId"),
                event.requireText("documentType"),
                event.requireText("ipfsCid"),
                Instant.parse(event.requireText("uploadedAt")));
    }
}
