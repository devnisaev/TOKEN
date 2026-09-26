package com.tokenrealty.document.kafka.port;

import java.time.Instant;
import java.util.UUID;

public interface DocumentUploadedPublisher {

    void publishDocumentUploaded(DocumentUploadedEvent event);

    record DocumentUploadedEvent(
            UUID documentId,
            UUID buildingId,
            UUID flatId,
            String documentType,
            String ipfsCid,
            String storageUrl,
            Instant uploadedAt
    ) {}
}
