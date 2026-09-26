package com.tokenrealty.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tokenrealty.kafka.dlq")
public class KafkaDlqProperties {

    private boolean enabled = false;
    private int maxRetries = 3;
    private long backoffMs = 1000L;
    private String suffix = ".dlq";

    public String dlqTopic(String sourceTopic) {
        return sourceTopic + suffix;
    }
}
