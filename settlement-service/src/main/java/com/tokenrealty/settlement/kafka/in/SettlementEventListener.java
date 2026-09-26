package com.tokenrealty.settlement.kafka.in;

import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.settlement.kafka.SettlementKafkaEventTypes;
import com.tokenrealty.settlement.service.SettlementSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SettlementEventListener {

    private final KafkaEventConsumer eventConsumer;
    private final SettlementSagaService sagaService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.order-matched}")
    public void onOrderMatched(String message) {
        ingest(message, SettlementKafkaEventTypes.ORDER_MATCHED, sagaService::onOrderMatched);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.payment-confirmed}")
    public void onPaymentConfirmed(String message) {
        ingest(message, SettlementKafkaEventTypes.PAYMENT_CONFIRMED, sagaService::onPaymentConfirmed);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.transfer-completed}")
    public void onTransferCompleted(String message) {
        ingest(message, SettlementKafkaEventTypes.TRANSFER_COMPLETED, sagaService::onTransferCompleted);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.trade-settled}")
    public void onTradeSettled(String message) {
        ingest(message, SettlementKafkaEventTypes.TRADE_SETTLED, sagaService::onTradeSettled);
    }

    private void ingest(String message, String eventType,
                        java.util.function.Consumer<com.tokenrealty.events.kafka.KafkaJsonEvent> handler) {
        eventConsumer.consume(message, eventType, "Settlement saga processing failed", handler);
    }
}
