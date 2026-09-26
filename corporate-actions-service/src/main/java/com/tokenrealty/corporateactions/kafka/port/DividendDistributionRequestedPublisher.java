package com.tokenrealty.corporateactions.kafka.port;

import com.tokenrealty.corporateactions.kafka.events.DividendDistributionRequestedEvent;

public interface DividendDistributionRequestedPublisher {

    void publish(DividendDistributionRequestedEvent event);
}
