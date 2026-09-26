package com.tokenrealty.compliance.kafka.in;

import com.tokenrealty.compliance.kafka.ComplianceKafkaEventTypes;
import com.tokenrealty.compliance.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.compliance.service.DocumentReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DocumentUploadedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final DocumentReviewService documentReviewService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.document-uploaded}")
    public void onDocumentUploaded(String message) {
        eventConsumer.consume(message, ComplianceKafkaEventTypes.DOCUMENT_UPLOADED,
                "Document review queue processing failed",
                event -> documentReviewService.queueForReview(DocumentUploadedCommand.from(event)));
    }
}
