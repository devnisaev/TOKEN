package com.tokenrealty.kafka.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {KafkaDlqIntegrationTest.TestApplication.class},
        properties = {
                "tokenrealty.kafka.dlq.enabled=true",
                "tokenrealty.kafka.dlq.max-retries=1",
                "tokenrealty.kafka.dlq.backoff-ms=10"
        })
@EmbeddedKafka(partitions = 1, topics = {
        KafkaDlqIntegrationTest.SOURCE_TOPIC,
        KafkaDlqIntegrationTest.SOURCE_TOPIC + ".dlq"
})
@DirtiesContext
@DisplayName("Kafka DLQ integration test")
class KafkaDlqIntegrationTest {

    static final String SOURCE_TOPIC = "tokenrealty.test.dlq.source.v1";

    @Autowired EmbeddedKafkaBroker embeddedKafka;
    @Autowired KafkaTemplate<Object, Object> kafkaTemplate;

    @Test
    @DisplayName("failing listener routes poison message to DLQ topic")
    void failingListener_routesToDlq() throws Exception {
        kafkaTemplate.send(SOURCE_TOPIC, "key", "poison-payload").get();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "dlq-test-consumer", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.subscribe(java.util.List.of(SOURCE_TOPIC + ".dlq"));

            ConsumerRecords<String, String> records = null;
            for (int attempt = 0; attempt < 20; attempt++) {
                records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    break;
                }
            }
            assertThat(records).isNotNull();
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            assertThat(records.iterator().next().value()).isEqualTo("poison-payload");
        }
    }

    @Configuration
    @Import({KafkaAutoConfiguration.class, KafkaDlqAutoConfiguration.class})
    static class TestApplication {

        @Bean
        FailingTestListener failingTestListener() {
            return new FailingTestListener();
        }
    }

    @Component
    static class FailingTestListener {

        private final AtomicInteger attempts = new AtomicInteger();

        @KafkaListener(topics = SOURCE_TOPIC, groupId = "dlq-integration-test")
        void onMessage(String message) {
            attempts.incrementAndGet();
            throw new IllegalStateException("Simulated processing failure");
        }
    }
}
