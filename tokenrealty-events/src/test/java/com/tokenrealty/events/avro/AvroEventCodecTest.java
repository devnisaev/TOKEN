package com.tokenrealty.events.avro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Avro event codec")
class AvroEventCodecTest {

    @Test
    void roundTripPaymentConfirmedPayload() throws Exception {
        AvroEventCodec codec = AvroEventCodec.fromClasspath("/avro/PaymentConfirmed.avsc");
        Map<String, Object> payload = Map.of(
                "paymentId", "11111111-1111-1111-1111-111111111111",
                "orderId", "22222222-2222-2222-2222-222222222222",
                "payerId", "33333333-3333-3333-3333-333333333333",
                "amount", Map.of("value", "100.00", "currency", "USDC"),
                "confirmedAt", "2026-01-15T10:00:00Z");

        byte[] encoded = codec.encode(payload);
        Map<String, Object> decoded = codec.decode(encoded);

        assertThat(decoded.get("paymentId")).isEqualTo(payload.get("paymentId"));
        assertThat(decoded.get("amount")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> amount = (Map<String, Object>) decoded.get("amount");
        assertThat(amount.get("value")).isEqualTo("100.00");
    }
}
