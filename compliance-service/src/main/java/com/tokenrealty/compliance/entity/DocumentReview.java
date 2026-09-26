package com.tokenrealty.compliance.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReview extends BaseEntity {

    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;

    @Column(name = "ipfs_cid", nullable = false, length = 128)
    private String ipfsCid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public enum ReviewStatus {
        PENDING,
        VERIFIED
    }
}
