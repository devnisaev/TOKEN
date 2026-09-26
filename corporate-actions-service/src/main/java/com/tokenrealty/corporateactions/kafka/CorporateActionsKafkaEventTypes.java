package com.tokenrealty.corporateactions.kafka;

public final class CorporateActionsKafkaEventTypes {

    public static final String RENT_COLLECTED = "tokenrealty.payment.rent.collected.v1";
    public static final String DIVIDEND_DISTRIBUTED = "tokenrealty.issuance.dividend.distributed.v1";
    public static final String DIVIDEND_DISTRIBUTION_REQUESTED =
            "tokenrealty.corporateactions.dividend.distribution-requested.v1";

    private CorporateActionsKafkaEventTypes() {
    }
}
