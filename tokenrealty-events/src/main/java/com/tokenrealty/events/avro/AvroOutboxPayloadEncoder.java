package com.tokenrealty.events.avro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Opt-in Avro encoding for outbox relay: inner payload bytes are base64-encoded in the JSON envelope.
 */
public final class AvroOutboxPayloadEncoder {

    public static final String PAYLOAD_ENCODING = "payloadEncoding";
    public static final String AVRO = "avro";

    private static final Logger log = LoggerFactory.getLogger(AvroOutboxPayloadEncoder.class);

    private AvroOutboxPayloadEncoder() {
    }

    public static String encodeEnvelope(String envelopeJson, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(envelopeJson);
            JsonNode payloadNode = root.get("payload");
            if (payloadNode == null || payloadNode.isNull()) {
                return envelopeJson;
            }

            String eventType = root.path("eventType").asText("");
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.convertValue(payloadNode, Map.class);
            byte[] avroBytes = encodePayload(eventType, root, payloadMap, objectMapper);
            String base64Payload = Base64.getEncoder().encodeToString(avroBytes);

            ObjectNode out = (ObjectNode) root.deepCopy();
            out.put("payload", base64Payload);
            out.put(PAYLOAD_ENCODING, AVRO);
            log.debug("Avro-encoded outbox payload for {} ({} bytes)", eventType, avroBytes.length);
            return objectMapper.writeValueAsString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to Avro-encode outbox envelope", ex);
        }
    }

    private static byte[] encodePayload(
            String eventType,
            JsonNode envelope,
            Map<String, Object> payloadMap,
            ObjectMapper objectMapper
    ) throws IOException {
        if (eventType.contains("payment.confirmed")) {
            return AvroEventCodec.fromClasspath("/avro/PaymentConfirmed.avsc").encode(payloadMap);
        }
        if (eventType.contains("trade.settled")) {
            return AvroEventCodec.fromClasspath("/avro/TradeSettled.avsc").encode(payloadMap);
        }

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("eventId", envelope.path("eventId").asText(""));
        wrapper.put("eventType", eventType);
        wrapper.put("occurredAt", envelope.path("occurredAt").asText(""));
        wrapper.put("traceId", envelope.path("traceId").asText(""));
        wrapper.put("payloadJson", objectMapper.writeValueAsString(payloadMap));
        return AvroEventCodec.fromClasspath("/avro/EventEnvelope.avsc").encode(wrapper);
    }
}
