package com.tokenrealty.registry.kafka.in;

import com.tokenrealty.registry.kafka.RegistryKafkaEventTypes;
import com.tokenrealty.registry.kafka.command.TransferCompletedCommand;
import com.tokenrealty.registry.service.FlatService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TransferCompletedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final FlatService flatService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.transfer-completed}")
    public void onTransferCompleted(String message) {
        eventConsumer.consume(message, RegistryKafkaEventTypes.TRANSFER_COMPLETED,
                "Transfer completed processing failed",
                event -> flatService.syncFromTransferCompleted(TransferCompletedCommand.from(event)));
    }
}
