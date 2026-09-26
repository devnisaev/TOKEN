package com.tokenrealty.indexer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class IndexerMetrics {

    private final AtomicLong blockLag = new AtomicLong(0);
    private final AtomicLong openMismatches = new AtomicLong(0);
    private final Counter eventsIndexed;

    public IndexerMetrics(MeterRegistry registry) {
        Gauge.builder("indexer.block.lag", blockLag, AtomicLong::get)
                .description("Latest chain block minus indexer cursor")
                .register(registry);
        Gauge.builder("indexer.balance.mismatches.open", openMismatches, AtomicLong::get)
                .description("Unresolved on-chain vs DB balance mismatches")
                .register(registry);
        eventsIndexed = Counter.builder("indexer.events.indexed.total")
                .description("On-chain log events persisted by the indexer")
                .register(registry);
    }

    public void setBlockLag(long lag) {
        blockLag.set(lag);
    }

    public void setOpenMismatches(long count) {
        openMismatches.set(count);
    }

    public void recordEventsIndexed(int count) {
        if (count > 0) {
            eventsIndexed.increment(count);
        }
    }
}
