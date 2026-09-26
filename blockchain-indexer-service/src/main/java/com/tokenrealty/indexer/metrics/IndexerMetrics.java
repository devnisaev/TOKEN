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
    private final Counter reconciliationRuns;
    private final Counter reconciliationMismatchesDetected;
    private final Counter reconciliationRemediated;

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
        reconciliationRuns = Counter.builder("indexer.reconciliation.runs.total")
                .description("Balance reconciliation job executions")
                .register(registry);
        reconciliationMismatchesDetected = Counter.builder("indexer.reconciliation.mismatches.detected.total")
                .description("On-chain vs DB balance mismatches detected")
                .register(registry);
        reconciliationRemediated = Counter.builder("indexer.reconciliation.remediated.total")
                .description("Balance mismatches remediated by syncing DB to chain")
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

    public void recordReconciliationRun() {
        reconciliationRuns.increment();
    }

    public void recordReconciliationMismatchDetected() {
        reconciliationMismatchesDetected.increment();
    }

    public void recordReconciliationRemediated() {
        reconciliationRemediated.increment();
    }
}
