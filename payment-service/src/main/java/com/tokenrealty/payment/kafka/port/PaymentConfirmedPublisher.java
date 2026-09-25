package com.tokenrealty.payment.kafka.port;

import com.tokenrealty.payment.kafka.events.PaymentConfirmedEvent;

public interface PaymentConfirmedPublisher {

    void publishPaymentConfirmed(PaymentConfirmedEvent event);
}
