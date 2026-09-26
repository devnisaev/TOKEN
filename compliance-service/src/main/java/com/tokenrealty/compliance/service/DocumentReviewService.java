package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.client.PropertyRegistryClient;
import com.tokenrealty.compliance.client.RegistryDocumentResponse;
import com.tokenrealty.compliance.dto.ComplianceDtos.DocumentReviewResponse;
import com.tokenrealty.compliance.entity.DocumentReview;
import com.tokenrealty.compliance.entity.DocumentReview.ReviewStatus;
import com.tokenrealty.compliance.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.compliance.repository.DocumentReviewRepository;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DocumentReviewService {

    private final DocumentReviewRepository reviewRepository;
    private final PropertyRegistryClient registryClient;

    @Transactional
    public void queueForReview(DocumentUploadedCommand command) {
        if (reviewRepository.existsByDocumentId(command.documentId())) {
            log.debug("Document review already queued for {}", command.documentId());
            return;
        }
        DocumentReview review = DocumentReview.builder()
                .documentId(command.documentId())
                .buildingId(command.buildingId())
                .flatId(command.flatId())
                .documentType(command.documentType())
                .ipfsCid(command.ipfsCid())
                .storageUrl(command.storageUrl())
                .uploadedAt(command.uploadedAt())
                .status(ReviewStatus.PENDING)
                .build();
        reviewRepository.save(review);
        log.info("Queued document {} for compliance review", command.documentId());
    }

    public List<DocumentReviewResponse> findPending() {
        return reviewRepository.findByStatusOrderByCreatedAtAsc(ReviewStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DocumentReviewResponse verify(UUID documentId) {
        DocumentReview review = reviewRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentReview", documentId));
        if (review.getStatus() == ReviewStatus.VERIFIED) {
            throw new ConflictException("Document review already completed for " + documentId);
        }
        RegistryDocumentResponse verified = registryClient.verifyDocument(documentId);
        review.setStatus(ReviewStatus.VERIFIED);
        review.setReviewedAt(Instant.now());
        reviewRepository.save(review);
        log.info("Verified document {} via Property Registry", documentId);
        return toResponse(review, verified.isVerified());
    }

    private DocumentReviewResponse toResponse(DocumentReview review) {
        return toResponse(review, review.getStatus() == ReviewStatus.VERIFIED);
    }

    private DocumentReviewResponse toResponse(DocumentReview review, boolean registryVerified) {
        return new DocumentReviewResponse(
                review.getId(),
                review.getDocumentId(),
                review.getBuildingId(),
                review.getFlatId(),
                review.getDocumentType(),
                review.getIpfsCid(),
                review.getStorageUrl(),
                review.getStatus(),
                registryVerified,
                review.getUploadedAt(),
                review.getReviewedAt(),
                review.getCreatedAt());
    }
}
