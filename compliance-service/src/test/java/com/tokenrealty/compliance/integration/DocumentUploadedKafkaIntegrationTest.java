package com.tokenrealty.compliance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.compliance.entity.DocumentReview.ReviewStatus;
import com.tokenrealty.compliance.kafka.ComplianceKafkaEventTypes;
import com.tokenrealty.compliance.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.compliance.service.DocumentReviewService;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
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
@DisplayName("Compliance document.uploaded Kafka integration test")
class DocumentUploadedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired DocumentReviewService documentReviewService;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("document.uploaded queues review for IPFS document")
    void documentUploaded_queuesReview() throws Exception {
        UUID documentId = UUID.randomUUID();

        ingestDocumentUploaded(documentId, "QmComplianceCid", null, UUID.randomUUID());

        assertThat(documentReviewService.findPending())
                .anyMatch(review -> review.documentId().equals(documentId)
                        && review.status() == ReviewStatus.PENDING);
    }

    @Test
    @DisplayName("document.uploaded queues review for private storageUrl document")
    void documentUploaded_queuesReviewForStorageUrl() throws Exception {
        UUID documentId = UUID.randomUUID();

        ingestDocumentUploaded(documentId, null, "s3://tokenrealty-private/kyc/id.pdf", UUID.randomUUID());

        assertThat(documentReviewService.findPending())
                .anyMatch(review -> review.documentId().equals(documentId));
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void documentUploaded_dedupesDuplicateEventId() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ingestDocumentUploaded(documentId, "QmDedupe", null, eventId);
        ingestDocumentUploaded(documentId, "QmDedupe", null, eventId);

        long pendingCount = documentReviewService.findPending().stream()
                .filter(review -> review.documentId().equals(documentId))
                .count();
        assertThat(pendingCount).isEqualTo(1);
    }

    private void ingestDocumentUploaded(UUID documentId, String ipfsCid, String storageUrl, UUID eventId)
            throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", documentId.toString());
        payload.put("buildingId", UUID.randomUUID().toString());
        payload.put("documentType", "TITLE_DEED");
        if (ipfsCid != null) {
            payload.put("ipfsCid", ipfsCid);
        }
        if (storageUrl != null) {
            payload.put("storageUrl", storageUrl);
        }
        payload.put("uploadedAt", Instant.now().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                ComplianceKafkaEventTypes.DOCUMENT_UPLOADED,
                "test-trace",
                payload));
        eventConsumer.consume(message, ComplianceKafkaEventTypes.DOCUMENT_UPLOADED,
                "Document uploaded processing failed",
                event -> documentReviewService.queueForReview(DocumentUploadedCommand.from(event)));
    }
}
