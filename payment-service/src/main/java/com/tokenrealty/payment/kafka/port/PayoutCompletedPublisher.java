package com.tokenrealty.payment.kafka.port;

import com.tokenrealty.payment.kafka.events.PayoutCompletedEvent;

public interface PayoutCompletedPublisher {

    void publishPayoutCompleted(PayoutCompletedEvent event);
}
