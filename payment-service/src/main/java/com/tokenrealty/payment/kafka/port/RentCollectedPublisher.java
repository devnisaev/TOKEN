package com.tokenrealty.payment.kafka.port;

import com.tokenrealty.payment.kafka.events.RentCollectedEvent;

public interface RentCollectedPublisher {

    void publishRentCollected(RentCollectedEvent event);
}
