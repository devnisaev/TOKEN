package com.tokenrealty.governance.kafka.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProposalClosedEvent(
        UUID proposalId,
        UUID flatId,
        BigDecimal quorumPct,
        int votesFor,
        int votesAgainst,
        Instant closedAt
) {
}
