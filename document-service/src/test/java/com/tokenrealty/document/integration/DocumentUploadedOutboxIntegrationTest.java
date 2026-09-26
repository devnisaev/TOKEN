package com.tokenrealty.document.integration;

import com.tokenrealty.document.client.PropertyRegistryClient;
import com.tokenrealty.document.dto.DocumentDtos.DocumentResponse;
import com.tokenrealty.document.dto.DocumentDtos.DocumentType;
import com.tokenrealty.document.dto.DocumentDtos.RegisterDocumentRequest;
import com.tokenrealty.document.kafka.DocumentKafkaEventTypes;
import com.tokenrealty.document.kafka.outbox.OutboxEventRepository;
import com.tokenrealty.document.service.DocumentUploadService;
import com.tokenrealty.document.storage.DocumentStorageRouter;
import com.tokenrealty.document.storage.DocumentStorageRouter.StorageResult;
import com.tokenrealty.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "tokenrealty.kafka.enabled=true")
@DisplayName("Document uploaded outbox integration test")
class DocumentUploadedOutboxIntegrationTest {

    @Autowired DocumentUploadService documentUploadService;
    @Autowired OutboxEventRepository outboxEventRepository;

    @MockitoBean DocumentStorageRouter storageRouter;
    @MockitoBean PropertyRegistryClient registryClient;

    @BeforeEach
    void clearOutbox() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("upload persists document.uploaded outbox row")
    void upload_createsOutboxRow() {
        UUID buildingId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "deed.pdf", "application/pdf", "pdf-content".getBytes());

        when(storageRouter.store(any(), eq("deed.pdf"), eq(DocumentType.TITLE_DEED)))
                .thenReturn(new StorageResult("bafyOutboxCid", null));
        when(registryClient.registerForBuilding(eq(buildingId), any(RegisterDocumentRequest.class)))
                .thenReturn(DocumentResponse.builder()
                        .id(documentId)
                        .documentName("Title deed")
                        .documentType(DocumentType.TITLE_DEED)
                        .ipfsCid("bafyOutboxCid")
                        .build());

        documentUploadService.upload(file, "Title deed", DocumentType.TITLE_DEED, buildingId, null);

        assertThat(outboxEventRepository.findAll())
                .anyMatch(event -> event.getEventType().equals(DocumentKafkaEventTypes.DOCUMENT_UPLOADED)
                        && event.getAggregateId().equals(documentId)
                        && event.getStatus() == OutboxStatus.PENDING);
    }
}
