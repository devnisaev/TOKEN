package com.tokenrealty.indexer.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
public class IndexerKafkaConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    NewTopic transferIndexedTopic(
            @Value("${tokenrealty.kafka.topic.transfer-indexed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic balanceMismatchTopic(
            @Value("${tokenrealty.kafka.topic.balance-mismatch}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }
}
