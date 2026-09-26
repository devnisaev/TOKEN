package com.tokenrealty.gateway.config;

import com.tokenrealty.gateway.filter.GatewayRateLimitFilter;
import com.tokenrealty.gateway.ratelimit.InMemoryRateLimitCounterStore;
import com.tokenrealty.gateway.ratelimit.RateLimitCounterStore;
import com.tokenrealty.gateway.ratelimit.RedisRateLimitCounterStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({GatewayRouteProperties.class, GatewayRateLimitProperties.class})
public class GatewayConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RateLimitCounterStore rateLimitCounterStore(
            GatewayRateLimitProperties properties,
            ObjectProvider<StringRedisTemplate> redisTemplate) {
        if (properties.getBackend() == GatewayRateLimitProperties.Backend.REDIS) {
            StringRedisTemplate redis = redisTemplate.getIfAvailable();
            if (redis != null) {
                return new RedisRateLimitCounterStore(redis);
            }
        }
        return new InMemoryRateLimitCounterStore();
    }

    @Bean
    GatewayRateLimitFilter gatewayRateLimitFilter(
            GatewayRateLimitProperties properties,
            RateLimitCounterStore counterStore) {
        return new GatewayRateLimitFilter(properties, counterStore);
    }
}
