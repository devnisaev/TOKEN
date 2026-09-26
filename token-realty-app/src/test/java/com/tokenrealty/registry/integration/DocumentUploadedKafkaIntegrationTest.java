package com.tokenrealty.registry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.registry.entity.PropertyDocument;
import com.tokenrealty.registry.kafka.RegistryKafkaEventTypes;
import com.tokenrealty.registry.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.registry.repository.PropertyDocumentRepository;
import com.tokenrealty.registry.service.DocumentService;
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
@DisplayName("Registry document.uploaded Kafka integration test")
class DocumentUploadedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired DocumentService documentService;
    @Autowired PropertyDocumentRepository documentRepository;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("document.uploaded backfills missing IPFS CID")
    void documentUploaded_backfillsCid() throws Exception {
        UUID documentId = saveDocument(PropertyDocument.DocumentType.TITLE_DEED);

        ingestDocumentUploaded(documentId, "QmKafkaCid123", null, UUID.randomUUID());

        PropertyDocument doc = documentRepository.findById(documentId).orElseThrow();
        assertThat(doc.getIpfsCid()).isEqualTo("QmKafkaCid123");
    }

    @Test
    @DisplayName("document.uploaded backfills missing storageUrl for private docs")
    void documentUploaded_backfillsStorageUrl() throws Exception {
        UUID documentId = saveDocument(PropertyDocument.DocumentType.KYC_DOCUMENT);

        String storageUrl = "s3://tokenrealty-private/kyc/passport.pdf";
        ingestDocumentUploaded(documentId, null, storageUrl, UUID.randomUUID());

        PropertyDocument doc = documentRepository.findById(documentId).orElseThrow();
        assertThat(doc.getStorageUrl()).isEqualTo(storageUrl);
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void documentUploaded_dedupesDuplicateEventId() throws Exception {
        UUID documentId = saveDocument(PropertyDocument.DocumentType.TITLE_DEED);
        UUID eventId = UUID.randomUUID();

        ingestDocumentUploaded(documentId, "QmDedupeCid", null, eventId);
        ingestDocumentUploaded(documentId, "QmDedupeCid", null, eventId);

        assertThat(documentRepository.findById(documentId).orElseThrow().getIpfsCid())
                .isEqualTo("QmDedupeCid");
    }

    private UUID saveDocument(PropertyDocument.DocumentType type) {
        return documentRepository.save(PropertyDocument.builder()
                .documentName("Test document")
                .documentType(type)
                .build()).getId();
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
                RegistryKafkaEventTypes.DOCUMENT_UPLOADED,
                "test-trace",
                payload));
        eventConsumer.consume(message, RegistryKafkaEventTypes.DOCUMENT_UPLOADED,
                "Document uploaded processing failed",
                event -> documentService.acknowledgeUpload(DocumentUploadedCommand.from(event)));
    }
}
