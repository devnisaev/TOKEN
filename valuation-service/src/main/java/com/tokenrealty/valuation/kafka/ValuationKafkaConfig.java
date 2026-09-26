package com.tokenrealty.valuation.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
public class ValuationKafkaConfig {

    @Bean
    NewTopic valuationUpdatedTopic(@Value("${tokenrealty.kafka.topic.valuation-updated}") String topic) {
        return new NewTopic(topic, 3, (short) 1);
    }
}
