package com.tokenrealty.gateway.ratelimit;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryRateLimitCounterStore implements RateLimitCounterStore {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String clientKey, int requestsPerMinute) {
        long windowStart = Instant.now().getEpochSecond() / 60;
        WindowCounter counter = counters.computeIfAbsent(clientKey, ignored -> new WindowCounter(windowStart));

        synchronized (counter) {
            if (counter.windowStart != windowStart) {
                counter.windowStart = windowStart;
                counter.count.set(0);
            }
            return counter.count.incrementAndGet() <= requestsPerMinute;
        }
    }

    private static final class WindowCounter {
        private long windowStart;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
