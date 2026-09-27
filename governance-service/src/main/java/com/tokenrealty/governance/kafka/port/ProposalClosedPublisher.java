package com.tokenrealty.governance.kafka.port;

import com.tokenrealty.governance.kafka.events.ProposalClosedEvent;

public interface ProposalClosedPublisher {

    void publish(ProposalClosedEvent event);
}
