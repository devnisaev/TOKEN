package com.tokenrealty.payment.service;

import com.tokenrealty.payment.dto.PaymentDtos.ConfirmPaymentRequest;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentAutoConfirmWorker unit tests")
class PaymentAutoConfirmWorkerTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentService paymentService;
    @InjectMocks PaymentAutoConfirmWorker worker;

    @Test
    @DisplayName("confirmPendingPayments confirms each pending payment")
    void confirmPendingPayments_confirmsEachPending() {
        UUID paymentId = UUID.randomUUID();
        Payment pending = Payment.builder()
                .orderId(UUID.randomUUID())
                .payerId(UUID.randomUUID())
                .payerWallet("0xBuyer")
                .amount(java.math.BigDecimal.TEN)
                .currency(PaymentCurrency.USDC)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .status(Payment.PaymentStatus.PENDING)
                .idempotencyKey("key-" + paymentId)
                .build();
        pending.setId(paymentId);
        when(paymentRepository.findTop20ByStatusOrderByCreatedAtAsc(Payment.PaymentStatus.PENDING))
                .thenReturn(List.of(pending));

        worker.confirmPendingPayments();

        verify(paymentService).confirm(eq(paymentId), any(ConfirmPaymentRequest.class));
    }
}
