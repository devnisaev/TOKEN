package com.tokenrealty.kafka.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KafkaDlqProperties unit tests")
class KafkaDlqPropertiesTest {

    @Test
    @DisplayName("dlqTopic appends configured suffix")
    void dlqTopic_appendsSuffix() {
        KafkaDlqProperties properties = new KafkaDlqProperties();
        properties.setSuffix(".dlq");

        assertThat(properties.dlqTopic("tokenrealty.marketplace.order.matched.v1"))
                .isEqualTo("tokenrealty.marketplace.order.matched.v1.dlq");
    }
}
