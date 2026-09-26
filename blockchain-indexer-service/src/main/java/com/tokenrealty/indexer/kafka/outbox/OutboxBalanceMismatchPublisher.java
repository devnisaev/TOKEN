package com.tokenrealty.indexer.kafka.outbox;

import com.tokenrealty.indexer.kafka.IndexerKafkaEventTypes;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxBalanceMismatchPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.balance-mismatch:" + IndexerKafkaEventTypes.BALANCE_MISMATCH + "}")
    private String balanceMismatchTopic;

    public void publishMismatch(
            UUID mismatchId,
            UUID contractId,
            String contractAddress,
            String walletAddress,
            long dbBalance,
            long chainBalance
    ) {
        OutboxPayload.start()
                .put("mismatchId", mismatchId)
                .put("contractId", contractId)
                .put("contractAddress", contractAddress)
                .put("walletAddress", walletAddress)
                .put("dbBalance", dbBalance)
                .put("chainBalance", chainBalance)
                .enqueue(outboxWriter, balanceMismatchTopic, mismatchId);
    }
}
