package com.tokenrealty.indexer.kafka;

public final class IndexerKafkaEventTypes {

    public static final String TRANSFER_INDEXED = "tokenrealty.indexer.transfer.indexed.v1";
    public static final String BALANCE_MISMATCH = "tokenrealty.indexer.balance.mismatch.v1";

    private IndexerKafkaEventTypes() {
    }
}
