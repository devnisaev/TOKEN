package com.tokenrealty.registry.kafka.command;

import com.tokenrealty.events.kafka.KafkaJsonEvent;

import java.time.Instant;
import java.util.UUID;

public record DocumentUploadedCommand(
        UUID documentId,
        UUID buildingId,
        UUID flatId,
        String documentType,
        String ipfsCid,
        String storageUrl,
        Instant uploadedAt
) {

    public static DocumentUploadedCommand from(KafkaJsonEvent event) {
        String ipfsCid = event.optionalText("ipfsCid");
        String storageUrl = event.optionalText("storageUrl");
        if (ipfsCid == null && storageUrl == null) {
            throw new IllegalArgumentException("Either ipfsCid or storageUrl is required");
        }
        return new DocumentUploadedCommand(
                event.requireUuid("documentId"),
                event.optionalUuid("buildingId"),
                event.optionalUuid("flatId"),
                event.requireText("documentType"),
                ipfsCid,
                storageUrl,
                Instant.parse(event.requireText("uploadedAt")));
    }
}
