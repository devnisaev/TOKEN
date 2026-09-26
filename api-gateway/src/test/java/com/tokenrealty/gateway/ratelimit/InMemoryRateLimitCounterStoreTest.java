package com.tokenrealty.gateway.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryRateLimitCounterStore unit tests")
class InMemoryRateLimitCounterStoreTest {

    @Test
    @DisplayName("allows requests up to limit then rejects")
    void tryConsume_enforcesLimit() {
        InMemoryRateLimitCounterStore store = new InMemoryRateLimitCounterStore();

        assertThat(store.tryConsume("127.0.0.1", 2)).isTrue();
        assertThat(store.tryConsume("127.0.0.1", 2)).isTrue();
        assertThat(store.tryConsume("127.0.0.1", 2)).isFalse();
    }
}
