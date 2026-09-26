package com.tokenrealty.reporting.kafka.in;

import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.reporting.kafka.ReportingKafkaEventTypes;
import com.tokenrealty.reporting.service.ReportingProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ReportingEventListener {

    private final KafkaEventConsumer eventConsumer;
    private final ReportingProjectionService projectionService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.trade-settled}")
    public void onTradeSettled(String message) {
        ingest(message, ReportingKafkaEventTypes.TRADE_SETTLED,
                projectionService::onTradeSettled);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.order-matched}")
    public void onOrderMatched(String message) {
        ingest(message, ReportingKafkaEventTypes.ORDER_MATCHED,
                projectionService::onOrderMatched);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.dividend-distributed}")
    public void onDividendDistributed(String message) {
        ingest(message, ReportingKafkaEventTypes.DIVIDEND_DISTRIBUTED,
                projectionService::onDividendDistributed);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.rent-collected}")
    public void onRentCollected(String message) {
        ingest(message, ReportingKafkaEventTypes.RENT_COLLECTED,
                projectionService::onRentCollected);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.flat-tokenized}")
    public void onFlatTokenized(String message) {
        ingest(message, ReportingKafkaEventTypes.FLAT_TOKENIZED,
                projectionService::onFlatTokenized);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.settlement-stuck}")
    public void onSettlementStuck(String message) {
        ingest(message, ReportingKafkaEventTypes.SETTLEMENT_STUCK,
                projectionService::onSettlementStuck);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.valuation-approved}")
    public void onValuationApproved(String message) {
        ingest(message, ReportingKafkaEventTypes.VALUATION_APPROVED,
                projectionService::onValuationApproved);
    }

    private void ingest(String message, String eventType,
                        java.util.function.Consumer<com.tokenrealty.events.kafka.KafkaJsonEvent> handler) {
        eventConsumer.consume(message, eventType, "Reporting projection failed", handler);
    }
}
