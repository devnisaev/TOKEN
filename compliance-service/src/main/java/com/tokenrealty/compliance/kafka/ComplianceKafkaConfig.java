package com.tokenrealty.compliance.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
public class ComplianceKafkaConfig {

    @Bean
    NewTopic kycApprovedTopic(@Value("${tokenrealty.kafka.topic.kyc-approved}") String topic) {
        return new NewTopic(topic, 3, (short) 1);
    }

    @Bean
    NewTopic kycRevokedTopic(@Value("${tokenrealty.kafka.topic.kyc-revoked}") String topic) {
        return new NewTopic(topic, 3, (short) 1);
    }
}
