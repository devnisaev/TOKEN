package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.client.PropertyRegistryClient;
import com.tokenrealty.compliance.client.RegistryDocumentResponse;
import com.tokenrealty.compliance.entity.DocumentReview;
import com.tokenrealty.compliance.entity.DocumentReview.ReviewStatus;
import com.tokenrealty.compliance.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.compliance.repository.DocumentReviewRepository;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentReviewService unit tests")
class DocumentReviewServiceTest {

    @Mock DocumentReviewRepository reviewRepository;
    @Mock PropertyRegistryClient registryClient;
    @InjectMocks DocumentReviewService documentReviewService;

    private UUID documentId;
    private DocumentUploadedCommand uploadCommand;
    private DocumentReview pendingReview;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        uploadCommand = new DocumentUploadedCommand(
                documentId,
                UUID.randomUUID(),
                null,
                "TITLE_DEED",
                "Qm123456789",
                Instant.parse("2025-09-25T16:00:00Z"));
        pendingReview = DocumentReview.builder()
                .documentId(documentId)
                .documentType("TITLE_DEED")
                .ipfsCid("Qm123456789")
                .status(ReviewStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("queueForReview saves pending review for new document")
    void queueForReview_savesPendingReview() {
        when(reviewRepository.existsByDocumentId(documentId)).thenReturn(false);
        when(reviewRepository.save(any(DocumentReview.class))).thenAnswer(inv -> inv.getArgument(0));

        documentReviewService.queueForReview(uploadCommand);

        ArgumentCaptor<DocumentReview> captor = ArgumentCaptor.forClass(DocumentReview.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getDocumentId()).isEqualTo(documentId);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    @DisplayName("queueForReview skips duplicate documentId")
    void queueForReview_skipsDuplicate() {
        when(reviewRepository.existsByDocumentId(documentId)).thenReturn(true);

        documentReviewService.queueForReview(uploadCommand);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("verify calls Registry and marks review verified")
    void verify_marksReviewVerified() {
        when(reviewRepository.findByDocumentId(documentId)).thenReturn(Optional.of(pendingReview));
        when(registryClient.verifyDocument(documentId)).thenReturn(
                new RegistryDocumentResponse(
                        documentId, "Title Deed", "TITLE_DEED", "Qm123456789",
                        null, 1024L, "application/pdf", true, "compliance-service",
                        "document-service", Instant.now()));
        when(reviewRepository.save(pendingReview)).thenReturn(pendingReview);

        var result = documentReviewService.verify(documentId);

        assertThat(result.verified()).isTrue();
        assertThat(pendingReview.getStatus()).isEqualTo(ReviewStatus.VERIFIED);
        assertThat(pendingReview.getReviewedAt()).isNotNull();
        verify(registryClient).verifyDocument(documentId);
    }

    @Test
    @DisplayName("verify throws ConflictException when already verified")
    void verify_throwsWhenAlreadyVerified() {
        pendingReview.setStatus(ReviewStatus.VERIFIED);
        when(reviewRepository.findByDocumentId(documentId)).thenReturn(Optional.of(pendingReview));

        assertThatThrownBy(() -> documentReviewService.verify(documentId))
                .isInstanceOf(ConflictException.class);

        verify(registryClient, never()).verifyDocument(any());
    }

    @Test
    @DisplayName("verify throws ResourceNotFoundException when review missing")
    void verify_throwsWhenReviewMissing() {
        when(reviewRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentReviewService.verify(documentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
