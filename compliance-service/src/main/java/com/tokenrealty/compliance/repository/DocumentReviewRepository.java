package com.tokenrealty.compliance.repository;

import com.tokenrealty.compliance.entity.DocumentReview;
import com.tokenrealty.compliance.entity.DocumentReview.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentReviewRepository extends JpaRepository<DocumentReview, UUID> {

    boolean existsByDocumentId(UUID documentId);

    Optional<DocumentReview> findByDocumentId(UUID documentId);

    List<DocumentReview> findByStatusOrderByCreatedAtAsc(ReviewStatus status);
}
