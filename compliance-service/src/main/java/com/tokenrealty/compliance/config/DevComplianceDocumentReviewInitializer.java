package com.tokenrealty.compliance.config;

import com.tokenrealty.compliance.entity.DocumentReview;
import com.tokenrealty.compliance.entity.DocumentReview.ReviewStatus;
import com.tokenrealty.compliance.repository.DocumentReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevComplianceDocumentReviewInitializer implements ApplicationRunner {

    /** Matches DevPropertyDataInitializer in token-realty-app. */
    public static final UUID DEMO_BUILDING_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID DEMO_FLAT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DEMO_DOCUMENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final DocumentReviewRepository reviewRepository;

    @Value("${tokenrealty.compliance.seed-dev-document-review:true}")
    private boolean seedDevDocumentReview;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedDevDocumentReview) {
            return;
        }
        if (reviewRepository.existsByDocumentId(DEMO_DOCUMENT_ID)) {
            return;
        }

        reviewRepository.save(DocumentReview.builder()
                .documentId(DEMO_DOCUMENT_ID)
                .buildingId(DEMO_BUILDING_ID)
                .flatId(DEMO_FLAT_ID)
                .documentType("TITLE_DEED")
                .ipfsCid("QmDemoTitleDeedCid")
                .uploadedAt(Instant.now())
                .status(ReviewStatus.PENDING)
                .build());
        log.info("Seeded pending document review for demo document {}", DEMO_DOCUMENT_ID);
    }
}
