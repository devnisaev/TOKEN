package com.tokenrealty.compliance.kafka;

public final class ComplianceKafkaEventTypes {

    public static final String KYC_APPROVED = "tokenrealty.compliance.investor.kyc-approved.v1";
    public static final String KYC_REVOKED = "tokenrealty.compliance.investor.kyc-revoked.v1";
    public static final String DOCUMENT_UPLOADED = "tokenrealty.document.document.uploaded.v1";

    private ComplianceKafkaEventTypes() {
    }
}
