package com.tokenrealty.search.kafka;

public final class SearchKafkaEventTypes {

    public static final String LISTING_CREATED = "tokenrealty.marketplace.listing.created.v1";
    public static final String FLAT_TOKENIZED = "tokenrealty.registry.flat.tokenized.v1";
    public static final String BUILDING_APPROVED = "tokenrealty.registry.building.approved.v1";
    public static final String VALUATION_UPDATED = "tokenrealty.valuation.updated.v1";

    private SearchKafkaEventTypes() {
    }
}
