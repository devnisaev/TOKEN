package com.tokenrealty.gateway.config;

import com.tokenrealty.gateway.filter.GatewayRateLimitFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({GatewayRouteProperties.class, GatewayRateLimitProperties.class})
public class GatewayConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    GatewayRateLimitFilter gatewayRateLimitFilter(GatewayRateLimitProperties properties) {
        return new GatewayRateLimitFilter(properties);
    }
}
