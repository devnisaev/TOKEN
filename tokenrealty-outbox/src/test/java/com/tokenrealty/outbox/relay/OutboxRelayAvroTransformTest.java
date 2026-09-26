package com.tokenrealty.outbox.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutboxRelay Avro transform resolver")
class OutboxRelayAvroTransformTest {

    @Test
    void resolvePayloadTransform_returnsNullForJson() {
        assertThat(OutboxRelay.resolvePayloadTransform("json", new ObjectMapper())).isNull();
    }

    @Test
    void resolvePayloadTransform_returnsEncoderForAvro() {
        var transform = OutboxRelay.resolvePayloadTransform("avro", new ObjectMapper());
        assertThat(transform).isNotNull();
        String encoded = transform.apply("{\"eventId\":\"abc\",\"payload\":{}}");
        assertThat(encoded).isNotBlank();
    }
}
