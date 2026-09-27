package com.tokenrealty.governance.kafka.outbox;

import com.tokenrealty.governance.kafka.GovernanceKafkaEventTypes;
import com.tokenrealty.governance.kafka.events.ProposalClosedEvent;
import com.tokenrealty.governance.kafka.port.ProposalClosedPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxProposalClosedPublisher implements ProposalClosedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.proposal-closed:" + GovernanceKafkaEventTypes.PROPOSAL_CLOSED + "}")
    private String proposalClosedTopic;

    @Override
    public void publish(ProposalClosedEvent event) {
        OutboxPayload.start()
                .put("proposalId", event.proposalId())
                .put("flatId", event.flatId())
                .put("quorumPct", event.quorumPct())
                .put("votesFor", event.votesFor())
                .put("votesAgainst", event.votesAgainst())
                .put("closedAt", event.closedAt())
                .enqueue(outboxWriter, proposalClosedTopic, event.proposalId());
    }
}
