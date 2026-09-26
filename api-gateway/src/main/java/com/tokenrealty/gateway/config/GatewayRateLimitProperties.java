package com.tokenrealty.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tokenrealty.gateway.rate-limit")
public class GatewayRateLimitProperties {

    public enum Backend {
        MEMORY,
        REDIS
    }

    private boolean enabled = true;
    private int requestsPerMinute = 120;
    private Backend backend = Backend.MEMORY;
}
