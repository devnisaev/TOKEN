package com.tokenrealty.audit.kafka;

public final class AuditKafkaEventTypes {

    public static final String KYC_APPROVED = "tokenrealty.compliance.investor.kyc-approved.v1";
    public static final String KYC_REVOKED = "tokenrealty.compliance.investor.kyc-revoked.v1";
    public static final String TRADE_SETTLED = "tokenrealty.marketplace.trade.settled.v1";
    public static final String DOCUMENT_UPLOADED = "tokenrealty.document.document.uploaded.v1";
    public static final String ORDER_MATCHED = "tokenrealty.marketplace.order.matched.v1";
    public static final String SETTLEMENT_RECOVERED = "tokenrealty.settlement.recovered.v1";
    public static final String VALUATION_APPROVED = "tokenrealty.valuation.approved.v1";

    private AuditKafkaEventTypes() {
    }
}
