package com.tokenrealty.registry.kafka.in;

import com.tokenrealty.registry.kafka.RegistryKafkaEventTypes;
import com.tokenrealty.registry.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.registry.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DocumentUploadedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final DocumentService documentService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.document-uploaded}")
    public void onDocumentUploaded(String message) {
        eventConsumer.consume(message, RegistryKafkaEventTypes.DOCUMENT_UPLOADED,
                "Document uploaded processing failed",
                event -> documentService.acknowledgeUpload(DocumentUploadedCommand.from(event)));
    }
}
