package com.tokenrealty.compliance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tokenrealty.compliance.entity.ComplianceRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ComplianceDtos {

    private ComplianceDtos() {
    }

    public record RegisterComplianceRequest(
            @NotNull UUID investorId,
            @NotBlank String walletAddress,
            String fullName,
            @Size(min = 2, max = 2) String countryCode,
            String kycProvider,
            String kycReferenceId
    ) {
    }

    public record ComplianceVerifyRequest(
            @NotNull Instant kycExpiresAt
    ) {
    }

    public record ComplianceRecordResponse(
            UUID id,
            UUID investorId,
            String walletAddress,
            String fullName,
            String countryCode,
            String kycProvider,
            Instant kycVerifiedAt,
            Instant kycExpiresAt,
            ComplianceRecord.ComplianceStatus status,
            String rejectionReason,
            Instant createdAt
    ) {
    }

    public record ComplianceCheckResponse(
            String walletAddress,
            @JsonProperty("isWhitelisted") boolean whitelisted,
            ComplianceRecord.ComplianceStatus status,
            UUID investorId,
            String countryCode,
            Instant expiresAt
    ) {
    }

    public record DocumentReviewResponse(
            UUID id,
            UUID documentId,
            UUID buildingId,
            UUID flatId,
            String documentType,
            String ipfsCid,
            String storageUrl,
            com.tokenrealty.compliance.entity.DocumentReview.ReviewStatus status,
            @JsonProperty("isVerified") boolean verified,
            Instant uploadedAt,
            Instant reviewedAt,
            Instant createdAt
    ) {
    }
}
