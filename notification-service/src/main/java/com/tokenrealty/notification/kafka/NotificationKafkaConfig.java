package com.tokenrealty.notification.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
public class NotificationKafkaConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    NewTopic flatTokenizedTopic(@Value("${tokenrealty.kafka.topic.flat-tokenized}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic listingCreatedTopic(@Value("${tokenrealty.kafka.topic.listing-created}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic orderMatchedTopic(@Value("${tokenrealty.kafka.topic.order-matched}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic paymentConfirmedTopic(@Value("${tokenrealty.kafka.topic.payment-confirmed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic transferCompletedTopic(@Value("${tokenrealty.kafka.topic.transfer-completed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic kycApprovedTopic(@Value("${tokenrealty.kafka.topic.kyc-approved}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic kycRevokedTopic(@Value("${tokenrealty.kafka.topic.kyc-revoked}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic tradeSettledTopic(@Value("${tokenrealty.kafka.topic.trade-settled}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic dividendDistributedTopic(@Value("${tokenrealty.kafka.topic.dividend-distributed}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }

    @Bean
    NewTopic rentCollectedTopic(@Value("${tokenrealty.kafka.topic.rent-collected}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }
}
