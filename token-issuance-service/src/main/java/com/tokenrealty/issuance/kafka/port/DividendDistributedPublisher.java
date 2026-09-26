package com.tokenrealty.issuance.kafka.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DividendDistributedPublisher {

    void publishDividendDistributed(DividendDistributedEvent event);

    record DividendDistributedEvent(
            UUID contractId,
            UUID flatId,
            String period,
            BigDecimal totalAmountUsd,
            List<HolderPayout> holderPayouts,
            Instant distributedAt
    ) {
    }

    record HolderPayout(
            UUID dividendPaymentId,
            UUID investorId,
            String walletAddress,
            BigDecimal amount,
            BigDecimal ownershipPct
    ) {
    }
}
