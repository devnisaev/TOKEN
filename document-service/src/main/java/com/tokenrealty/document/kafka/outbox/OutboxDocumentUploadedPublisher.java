package com.tokenrealty.document.kafka.outbox;

import com.tokenrealty.document.kafka.DocumentKafkaEventTypes;
import com.tokenrealty.document.kafka.port.DocumentUploadedPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxDocumentUploadedPublisher implements DocumentUploadedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.document-uploaded:" + DocumentKafkaEventTypes.DOCUMENT_UPLOADED + "}")
    private String documentUploadedTopic;

    @Override
    public void publishDocumentUploaded(DocumentUploadedEvent event) {
        OutboxPayload.start()
                .put("documentId", event.documentId())
                .put("buildingId", event.buildingId())
                .put("flatId", event.flatId())
                .put("documentType", event.documentType())
                .put("ipfsCid", event.ipfsCid())
                .put("uploadedAt", event.uploadedAt().toString())
                .enqueue(outboxWriter, documentUploadedTopic, event.documentId().toString());
    }
}
