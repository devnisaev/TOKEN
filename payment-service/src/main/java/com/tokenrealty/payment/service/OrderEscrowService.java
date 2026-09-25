package com.tokenrealty.payment.service;

import com.tokenrealty.payment.kafka.command.OrderMatchedCommand;
import com.tokenrealty.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEscrowService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public void ensureEscrowLinked(OrderMatchedCommand command) {
        if (paymentRepository.existsByOrderId(command.orderId())) {
            log.debug("Escrow already exists for order {}", command.orderId());
            return;
        }
        log.warn("Order {} matched without payment record — sync PaymentClient path may have failed",
                command.orderId());
    }
}
