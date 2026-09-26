package com.tokenrealty.events.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kafka serialization properties")
class KafkaSerializationPropertiesTest {

    @Test
    void avroEnabledWhenFormatIsAvro() {
        var props = new KafkaSerializationProperties("avro", "http://localhost:8092");
        assertThat(props.avroEnabled()).isTrue();
    }

    @Test
    void jsonDefaultsDisableAvro() {
        assertThat(KafkaSerializationProperties.jsonDefaults().avroEnabled()).isFalse();
    }
}
