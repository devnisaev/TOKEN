package com.tokenrealty.issuance.kafka.port;

import java.time.Instant;
import java.util.UUID;

public interface TransferCompletedPublisher {

    void publishTransferCompleted(TransferCompletedEvent event);

    record TransferCompletedEvent(
            UUID transferId,
            UUID contractId,
            UUID flatId,
            UUID orderId,
            UUID tradeId,
            UUID paymentId,
            String fromWallet,
            String toWallet,
            long tokenAmount,
            String txHash,
            Instant completedAt
    ) {
    }
}
