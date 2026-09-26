package com.tokenrealty.registry.entity;

import jakarta.persistence.*;
import lombok.*;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "property_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id")
    private Flat flat;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    // IPFS content identifier — immutable once stored
    @Column(name = "ipfs_cid")
    private String ipfsCid;

    // S3/MinIO fallback URL for non-public docs
    @Column(name = "storage_url")
    private String storageUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verified_by")
    private String verifiedBy;

    public enum DocumentType {
        TITLE_DEED,
        FLOOR_PLAN,
        BUILDING_PERMIT,
        INSPECTION_REPORT,
        VALUATION_REPORT,
        SPV_REGISTRATION,
        ARTICLES_OF_INCORPORATION,
        ENVIRONMENTAL_AUDIT,
        KYC_DOCUMENT,
        LEASE_AGREEMENT,
        INSURANCE_POLICY,
        OTHER
    }
}
