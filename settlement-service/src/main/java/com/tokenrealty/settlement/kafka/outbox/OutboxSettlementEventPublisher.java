package com.tokenrealty.settlement.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.settlement.kafka.SettlementKafkaEventTypes;
import com.tokenrealty.settlement.kafka.port.SettlementEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxSettlementEventPublisher implements SettlementEventPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.settlement-stuck:" + SettlementKafkaEventTypes.SETTLEMENT_STUCK + "}")
    private String settlementStuckTopic;

    @Value("${tokenrealty.kafka.topic.settlement-recovered:" + SettlementKafkaEventTypes.SETTLEMENT_RECOVERED + "}")
    private String settlementRecoveredTopic;

    @Override
    public void publishStuck(SettlementStuckEvent event) {
        OutboxPayload.start()
                .put("sagaId", event.sagaId())
                .put("orderId", event.orderId())
                .put("currentStep", event.currentStep().name())
                .put("stuckAt", event.stuckAt().toString())
                .enqueue(outboxWriter, settlementStuckTopic, event.orderId().toString());
    }

    @Override
    public void publishRecovered(SettlementRecoveredEvent event) {
        OutboxPayload.start()
                .put("sagaId", event.sagaId())
                .put("orderId", event.orderId())
                .put("currentStep", event.currentStep().name())
                .put("recoveredAt", event.recoveredAt().toString())
                .enqueue(outboxWriter, settlementRecoveredTopic, event.orderId().toString());
    }
}
