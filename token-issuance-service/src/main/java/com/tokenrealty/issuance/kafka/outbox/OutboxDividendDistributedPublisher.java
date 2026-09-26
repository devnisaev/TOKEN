package com.tokenrealty.issuance.kafka.outbox;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.port.DividendDistributedPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxDividendDistributedPublisher implements DividendDistributedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.dividend-distributed:" + IssuanceKafkaEventTypes.DIVIDEND_DISTRIBUTED + "}")
    private String dividendDistributedTopic;

    @Override
    public void publishDividendDistributed(DividendDistributedEvent event) {
        List<Map<String, Object>> holderPayouts = event.holderPayouts().stream()
                .map(holder -> Map.<String, Object>of(
                        "investorId", holder.investorId(),
                        "walletAddress", holder.walletAddress(),
                        "amount", holder.amount().toPlainString(),
                        "ownershipPct", holder.ownershipPct().toPlainString()))
                .toList();

        OutboxPayload.start()
                .put("contractId", event.contractId())
                .put("flatId", event.flatId())
                .put("period", event.period())
                .put("totalAmount", Map.of(
                        "value", event.totalAmountUsd().toPlainString(),
                        "currency", "USDC"))
                .put("holderPayouts", holderPayouts)
                .put("distributedAt", event.distributedAt().toString())
                .enqueue(outboxWriter, dividendDistributedTopic, event.contractId());
    }
}
