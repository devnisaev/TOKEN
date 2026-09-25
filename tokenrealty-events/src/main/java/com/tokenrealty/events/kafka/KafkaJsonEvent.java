package com.tokenrealty.events.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared envelope + payload parser for Kafka consumers.
 */
public final class KafkaJsonEvent {

    private static final Logger log = LoggerFactory.getLogger(KafkaJsonEvent.class);

    private final JsonNode envelope;
    private final JsonNode payload;

    private KafkaJsonEvent(JsonNode envelope, JsonNode payload) {
        this.envelope = envelope;
        this.payload = payload;
    }

    public static void consume(ObjectMapper mapper, String message, String failure, Handler handler) {
        try {
            JsonNode root = mapper.readTree(message);
            handler.accept(new KafkaJsonEvent(root, root.get("payload")));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process Kafka event: {}", ex.getMessage(), ex);
            throw new IllegalStateException(failure, ex);
        }
    }

    public UUID eventId() {
        return readUuid(envelope, "eventId");
    }

    public String eventType() {
        return envelope.get("eventType").asText();
    }

    public JsonNode payload() {
        return payload;
    }

    public String envelopeTraceId() {
        return envelope.hasNonNull("traceId") ? envelope.get("traceId").asText() : null;
    }

    public UUID requireUuid(String field) {
        return readUuid(payload, field);
    }

    public UUID optionalUuid(String field) {
        return payload.hasNonNull(field) ? readUuid(payload, field) : null;
    }

    public String requireText(String field) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return node.asText();
    }

    public String optionalText(String field) {
        return payload.hasNonNull(field) ? payload.get(field).asText() : null;
    }

    public int requireInt(String field) {
        return requireNode(payload, field).asInt();
    }

    public BigDecimal requireDecimal(String field) {
        return new BigDecimal(requireNode(payload, field).asText());
    }

    public Instant optionalInstant(String field) {
        return payload.hasNonNull(field) ? Instant.parse(payload.get(field).asText()) : null;
    }

    public List<UUID> requireUuidList(String field) {
        JsonNode node = requireNode(payload, field);
        List<UUID> ids = new ArrayList<>();
        for (JsonNode item : node) {
            ids.add(UUID.fromString(item.asText()));
        }
        return List.copyOf(ids);
    }

    public <T> T payloadAs(ObjectMapper mapper, Class<T> type) throws Exception {
        return mapper.treeToValue(payload, type);
    }

    private static JsonNode requireNode(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return node;
    }

    private static UUID readUuid(JsonNode parent, String field) {
        JsonNode node = requireNode(parent, field);
        if (node.isObject() && node.has("value")) {
            return UUID.fromString(node.get("value").asText());
        }
        return UUID.fromString(node.asText());
    }

    @FunctionalInterface
    public interface Handler {
        void accept(KafkaJsonEvent event) throws Exception;
    }
}
