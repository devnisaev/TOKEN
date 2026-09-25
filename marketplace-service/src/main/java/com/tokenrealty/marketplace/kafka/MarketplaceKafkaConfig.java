package com.tokenrealty.marketplace.kafka;

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
public class MarketplaceKafkaConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    NewTopic listingCreatedTopic(
            @Value("${tokenrealty.kafka.topic.listing-created}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic orderMatchedTopic(
            @Value("${tokenrealty.kafka.topic.order-matched}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic tradeSettledTopic(
            @Value("${tokenrealty.kafka.topic.trade-settled}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic flatTokenizedTopic(
            @Value("${tokenrealty.kafka.topic.flat-tokenized}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic paymentConfirmedTopic(
            @Value("${tokenrealty.kafka.topic.payment-confirmed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic transferCompletedTopic(
            @Value("${tokenrealty.kafka.topic.transfer-completed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }
}
