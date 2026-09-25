package com.tokenrealty.outbox;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPayloadTest {

    @Test
    void buildsMapAndOmitsNulls() {
        UUID id = UUID.randomUUID();
        var payload = OutboxPayload.start()
                .put("name", "test")
                .put("id", id)
                .put("count", 3)
                .put("active", true)
                .put("amount", new BigDecimal("10.50"))
                .putIfNotNull("optional", (String) null)
                .putIfNotBlank("blank", "  ")
                .putUuidList("ids", List.of(id));

        assertThat(payload.toMap())
                .containsEntry("name", "test")
                .containsEntry("id", id.toString())
                .containsEntry("count", 3)
                .containsEntry("active", true)
                .containsEntry("amount", "10.50")
                .containsEntry("ids", List.of(id.toString()))
                .doesNotContainKey("optional")
                .doesNotContainKey("blank");
    }
}
