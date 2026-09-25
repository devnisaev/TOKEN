package com.tokenrealty.issuance.kafka.outbox;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.port.TransferCompletedPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxTransferCompletedPublisher implements TransferCompletedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.transfer-completed:" + IssuanceKafkaEventTypes.TRANSFER_COMPLETED + "}")
    private String transferCompletedTopic;

    @Override
    public void publishTransferCompleted(TransferCompletedEvent event) {
        OutboxPayload.start()
                .put("transferId", event.transferId())
                .put("contractId", event.contractId())
                .put("flatId", event.flatId())
                .put("orderId", event.orderId())
                .putIfPresent("tradeId", event.tradeId())
                .putIfPresent("paymentId", event.paymentId())
                .put("fromWallet", event.fromWallet())
                .put("toWallet", event.toWallet())
                .put("tokenAmount", event.tokenAmount())
                .putIfNotBlank("txHash", event.txHash())
                .put("completedAt", event.completedAt().toString())
                .enqueue(outboxWriter, transferCompletedTopic, event.contractId());
    }
}
