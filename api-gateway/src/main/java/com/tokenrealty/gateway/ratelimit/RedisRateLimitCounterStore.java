package com.tokenrealty.gateway.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

public class RedisRateLimitCounterStore implements RateLimitCounterStore {

    private static final String KEY_PREFIX = "gateway:ratelimit:";

    private final StringRedisTemplate redis;

    public RedisRateLimitCounterStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryConsume(String clientKey, int requestsPerMinute) {
        long windowStart = Instant.now().getEpochSecond() / 60;
        String key = KEY_PREFIX + clientKey + ":" + windowStart;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofMinutes(2));
        }
        return count != null && count <= requestsPerMinute;
    }
}
