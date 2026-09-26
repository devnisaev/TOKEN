package com.tokenrealty.notification.kafka;

public final class NotificationKafkaEventTypes {

    public static final String BUILDING_APPROVED = "tokenrealty.registry.building.approved.v1";
    public static final String FLAT_TOKENIZED = "tokenrealty.registry.flat.tokenized.v1";
    public static final String LISTING_CREATED = "tokenrealty.marketplace.listing.created.v1";
    public static final String ORDER_MATCHED = "tokenrealty.marketplace.order.matched.v1";
    public static final String PAYMENT_CONFIRMED = "tokenrealty.payment.payment.confirmed.v1";
    public static final String TRANSFER_COMPLETED = "tokenrealty.issuance.transfer.completed.v1";
    public static final String KYC_APPROVED = "tokenrealty.compliance.investor.kyc-approved.v1";
    public static final String KYC_REVOKED = "tokenrealty.compliance.investor.kyc-revoked.v1";
    public static final String TRADE_SETTLED = "tokenrealty.marketplace.trade.settled.v1";
    public static final String DIVIDEND_DISTRIBUTED = "tokenrealty.issuance.dividend.distributed.v1";
    public static final String RENT_COLLECTED = "tokenrealty.payment.rent.collected.v1";
    public static final String RENT_DUE = "tokenrealty.rental.rent.due.v1";
    public static final String LEASE_EXPIRED = "tokenrealty.rental.lease.expired.v1";
    public static final String DOCUMENT_UPLOADED = "tokenrealty.document.document.uploaded.v1";
    public static final String SETTLEMENT_STUCK = "tokenrealty.settlement.stuck.v1";
    public static final String SETTLEMENT_RECOVERED = "tokenrealty.settlement.recovered.v1";
    public static final String VALUATION_APPROVED = "tokenrealty.valuation.approved.v1";

    private NotificationKafkaEventTypes() {
    }
}
