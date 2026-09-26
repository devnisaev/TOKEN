package com.tokenrealty.gateway.ratelimit;

public interface RateLimitCounterStore {

    boolean tryConsume(String clientKey, int requestsPerMinute);
}
