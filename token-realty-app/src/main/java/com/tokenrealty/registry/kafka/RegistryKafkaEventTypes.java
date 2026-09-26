package com.tokenrealty.registry.kafka;

public final class RegistryKafkaEventTypes {

    public static final String BUILDING_APPROVED = "tokenrealty.registry.building.approved.v1";
    public static final String FLAT_TOKENIZED = "tokenrealty.registry.flat.tokenized.v1";
    public static final String DOCUMENT_UPLOADED = "tokenrealty.document.document.uploaded.v1";
    public static final String TRANSFER_COMPLETED = "tokenrealty.issuance.transfer.completed.v1";

    private RegistryKafkaEventTypes() {
    }
}
