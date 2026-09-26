package com.tokenrealty.payment.kafka;

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
public class PaymentKafkaConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    NewTopic paymentConfirmedTopic(
            @Value("${tokenrealty.kafka.topic.payment-confirmed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic rentCollectedTopic(
            @Value("${tokenrealty.kafka.topic.rent-collected}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic orderMatchedTopic(
            @Value("${tokenrealty.kafka.topic.order-matched}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic dividendDistributedTopic(
            @Value("${tokenrealty.kafka.topic.dividend-distributed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic payoutCompletedTopic(
            @Value("${tokenrealty.kafka.topic.payout-completed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }
}
