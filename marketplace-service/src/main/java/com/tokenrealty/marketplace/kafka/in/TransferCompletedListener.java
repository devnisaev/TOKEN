package com.tokenrealty.marketplace.kafka.in;

import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.command.TransferCompletedCommand;
import com.tokenrealty.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TransferCompletedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final OrderService orderService;
    private final PaymentClient paymentClient;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.transfer-completed}")
    public void onTransferCompleted(String message) {
        eventConsumer.consume(message, MarketplaceKafkaEventTypes.TRANSFER_COMPLETED,
                "Transfer completed processing failed",
                event -> {
                    TransferCompletedCommand command = TransferCompletedCommand.from(event);
                    UUID paymentId = orderService.settleFromTransfer(command);
                    if (paymentId != null) {
                        paymentClient.releaseEscrow(paymentId);
                        log.info("Released escrow for payment {} after transfer {}", paymentId, command.transferId());
                    }
                });
    }
}
