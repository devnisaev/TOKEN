package com.tokenrealty.settlement.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableKafka
@EnableScheduling
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
public class SettlementKafkaConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    NewTopic settlementStuckTopic(@Value("${tokenrealty.kafka.topic.settlement-stuck}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic settlementRecoveredTopic(@Value("${tokenrealty.kafka.topic.settlement-recovered}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }
}
