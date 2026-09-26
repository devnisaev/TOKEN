package com.tokenrealty.events.avro;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Avro outbox payload encoder")
class AvroOutboxPayloadEncoderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void encodesInnerPayloadAsBase64AvroBytes() throws Exception {
        String envelope = """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventType": "tokenrealty.payment.payment.confirmed.v1",
                  "occurredAt": "2026-01-15T10:00:00Z",
                  "traceId": "trace-1",
                  "payload": {
                    "paymentId": "22222222-2222-2222-2222-222222222222",
                    "orderId": "33333333-3333-3333-3333-333333333333",
                    "payerId": "44444444-4444-4444-4444-444444444444",
                    "amount": { "value": "100.00", "currency": "USDC" },
                    "confirmedAt": "2026-01-15T10:00:00Z"
                  }
                }
                """;

        String encoded = AvroOutboxPayloadEncoder.encodeEnvelope(envelope, objectMapper);
        var node = objectMapper.readTree(encoded);

        assertThat(node.path(AvroOutboxPayloadEncoder.PAYLOAD_ENCODING).asText())
                .isEqualTo(AvroOutboxPayloadEncoder.AVRO);
        assertThat(node.path("payload").asText()).isNotBlank();
        assertThat(node.path("eventType").asText()).contains("payment.confirmed");
    }
}
