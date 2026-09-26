package com.tokenrealty.corporateactions.kafka.port;

import com.tokenrealty.corporateactions.kafka.events.StockSplitRequestedEvent;

public interface StockSplitRequestedPublisher {

    void publish(StockSplitRequestedEvent event);
}
