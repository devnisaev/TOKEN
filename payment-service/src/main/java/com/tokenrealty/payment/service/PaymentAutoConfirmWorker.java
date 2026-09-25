package com.tokenrealty.payment.service;

import com.tokenrealty.payment.dto.PaymentDtos.ConfirmPaymentRequest;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.payment.auto-confirm.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PaymentAutoConfirmWorker {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${tokenrealty.payment.auto-confirm.poll-ms:3000}")
    public void confirmPendingPayments() {
        paymentRepository.findTop20ByStatusOrderByCreatedAtAsc(Payment.PaymentStatus.PENDING)
                .forEach(this::confirmOne);
    }

    private void confirmOne(Payment payment) {
        try {
            String txHash = "0xSIMULATED_" + payment.getId().toString().replace("-", "");
            paymentService.confirm(payment.getId(), new ConfirmPaymentRequest(txHash));
            log.info("Auto-confirmed payment id={} orderId={}", payment.getId(), payment.getOrderId());
        } catch (Exception ex) {
            log.warn("Auto-confirm failed for payment {}: {}", payment.getId(), ex.getMessage());
        }
    }
}
