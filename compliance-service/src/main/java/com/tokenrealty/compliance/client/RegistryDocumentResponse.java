package com.tokenrealty.compliance.client;

import java.time.Instant;
import java.util.UUID;

public record RegistryDocumentResponse(
        UUID id,
        String documentName,
        String documentType,
        String ipfsCid,
        String storageUrl,
        Long fileSizeBytes,
        String mimeType,
        Boolean isVerified,
        String verifiedBy,
        String uploadedBy,
        Instant createdAt
) {
}
