package com.tokenrealty.registry.kafka.in;

import com.tokenrealty.registry.kafka.RegistryKafkaEventTypes;
import com.tokenrealty.registry.kafka.command.ValuationUpdatedCommand;
import com.tokenrealty.registry.service.ValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ValuationUpdatedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final ValuationService valuationService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.valuation-updated}")
    public void onValuationUpdated(String message) {
        eventConsumer.consume(message, RegistryKafkaEventTypes.VALUATION_UPDATED,
                "Valuation updated processing failed",
                event -> valuationService.syncFromValuationUpdated(ValuationUpdatedCommand.from(event)));
    }
}
