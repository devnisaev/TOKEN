package com.tokenrealty.compliance.integration;

import com.tokenrealty.compliance.client.PropertyRegistryClient;
import com.tokenrealty.compliance.client.RegistryDocumentResponse;
import com.tokenrealty.compliance.entity.DocumentReview.ReviewStatus;
import com.tokenrealty.compliance.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.compliance.service.DocumentReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Data room loop integration test")
class DataRoomLoopIntegrationTest {

    @Autowired DocumentReviewService documentReviewService;
    @MockitoBean PropertyRegistryClient propertyRegistryClient;

    @Test
    void uploadEventToVerify_closesComplianceLoop() {
        UUID documentId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();
        DocumentUploadedCommand command = new DocumentUploadedCommand(
                documentId,
                buildingId,
                null,
                "TITLE_DEED",
                "QmTestCid123",
                null,
                Instant.parse("2025-09-25T16:00:00Z"));

        documentReviewService.queueForReview(command);

        assertThat(documentReviewService.findPending())
                .anyMatch(review -> review.documentId().equals(documentId)
                        && review.status() == ReviewStatus.PENDING);

        when(propertyRegistryClient.verifyDocument(documentId)).thenReturn(
                new RegistryDocumentResponse(
                        documentId, "Title Deed", "TITLE_DEED", "QmTestCid123",
                        null, 1024L, "application/pdf", true, "compliance-service",
                        "document-service", Instant.now()));

        var verified = documentReviewService.verify(documentId);

        assertThat(verified.verified()).isTrue();
        assertThat(verified.status()).isEqualTo(ReviewStatus.VERIFIED);
        assertThat(documentReviewService.findPending())
                .noneMatch(review -> review.documentId().equals(documentId));
        verify(propertyRegistryClient).verifyDocument(documentId);
    }
}
