package com.tokenrealty.outbox.relay;

import java.time.Instant;
import java.util.UUID;

public interface OutboxRelayTarget {

    UUID getId();

    String getEventType();

    UUID getAggregateId();

    String getPayload();

    int getRetryCount();

    void setRetryCount(int retryCount);

    void markPublished(Instant publishedAt);

    void markFailed();
}
