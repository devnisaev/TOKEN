package com.tokenrealty.document.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

public final class DocumentDtos {

    private DocumentDtos() {
    }

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

    @Builder
    public record RegisterDocumentRequest(
            String documentName,
            DocumentType documentType,
            String ipfsCid,
            String storageUrl,
            Long fileSizeBytes,
            String mimeType
    ) {}

    @Builder
    public record DocumentResponse(
            UUID id,
            String documentName,
            DocumentType documentType,
            String ipfsCid,
            String storageUrl,
            Long fileSizeBytes,
            String mimeType,
            Boolean isVerified,
            String verifiedBy,
            String uploadedBy,
            Instant createdAt
    ) {}
}
