package com.tokenrealty.outbox;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fluent map builder for outbox Kafka payload bodies. Use with {@link OutboxWriter#enqueue}.
 */
public final class OutboxPayload {

    private final Map<String, Object> values = new LinkedHashMap<>();

    public static OutboxPayload start() {
        return new OutboxPayload();
    }

    public OutboxPayload put(String key, String value) {
        values.put(key, value);
        return this;
    }

    public OutboxPayload put(String key, UUID id) {
        values.put(key, id.toString());
        return this;
    }

    public OutboxPayload put(String key, int value) {
        values.put(key, value);
        return this;
    }

    public OutboxPayload put(String key, long value) {
        values.put(key, value);
        return this;
    }

    public OutboxPayload put(String key, boolean value) {
        values.put(key, value);
        return this;
    }

    public OutboxPayload put(String key, BigDecimal amount) {
        values.put(key, amount.toPlainString());
        return this;
    }

    public OutboxPayload put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public OutboxPayload putIfNotNull(String key, String value) {
        if (value != null) {
            values.put(key, value);
        }
        return this;
    }

    public OutboxPayload putIfNotNull(String key, UUID id) {
        if (id != null) {
            values.put(key, id.toString());
        }
        return this;
    }

    public OutboxPayload putIfNotNull(String key, Integer value) {
        if (value != null) {
            values.put(key, value);
        }
        return this;
    }

    public OutboxPayload putIfNotNull(String key, BigDecimal amount) {
        if (amount != null) {
            values.put(key, amount.toPlainString());
        }
        return this;
    }

    /** Alias for {@link #putIfNotNull(String, UUID)} etc. */
    public OutboxPayload putIfPresent(String key, UUID id) {
        return putIfNotNull(key, id);
    }

    public OutboxPayload putIfPresent(String key, String value) {
        return putIfNotNull(key, value);
    }

    public OutboxPayload putIfPresent(String key, BigDecimal amount) {
        return putIfNotNull(key, amount);
    }

    public OutboxPayload putIfNotBlank(String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value.trim());
        }
        return this;
    }

    public OutboxPayload putUuidList(String key, List<UUID> ids) {
        values.put(key, ids.stream().map(UUID::toString).toList());
        return this;
    }

    public Map<String, Object> toMap() {
        return values;
    }

    public void enqueue(OutboxWriter writer, String topic, UUID partitionKey) {
        writer.enqueue(topic, partitionKey.toString(), values, partitionKey.toString());
    }

    public void enqueue(OutboxWriter writer, String topic, UUID partitionKey, UUID traceId) {
        writer.enqueue(topic, partitionKey.toString(), values, traceId.toString());
    }

    public void enqueue(OutboxWriter writer, String topic, String partitionKey) {
        writer.enqueue(topic, partitionKey, values, partitionKey);
    }

    public void enqueue(OutboxWriter writer, String topic, String partitionKey, String traceId) {
        writer.enqueue(topic, partitionKey, values, traceId);
    }
}
