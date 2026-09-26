package com.tokenrealty.events.kafka;

/**
 * Opt-in Kafka payload serialization settings. JSON envelope remains the default.
 */
public record KafkaSerializationProperties(
        String format,
        String schemaRegistryUrl
) {
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_AVRO = "avro";

    public boolean avroEnabled() {
        return FORMAT_AVRO.equalsIgnoreCase(format);
    }

    public static KafkaSerializationProperties jsonDefaults() {
        return new KafkaSerializationProperties(FORMAT_JSON, "http://localhost:8092");
    }
}
