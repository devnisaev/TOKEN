package com.tokenrealty.payment.kafka.events;

import com.tokenrealty.payment.entity.Payout;

import java.time.Instant;
import java.util.UUID;

public record PayoutCompletedEvent(
        UUID payoutId,
        UUID dividendPaymentId,
        UUID recipientInvestorId,
        String recipientWallet,
        Payout.PayoutPurpose purpose,
        String txHash,
        Instant completedAt
) {
}
