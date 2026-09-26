package com.tokenrealty.payment.service;

import com.tokenrealty.payment.blockchain.PaymentBlockchainService;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.Payment.PaymentStatus;
import com.tokenrealty.payment.repository.PaymentRepository;
import com.tokenrealty.payment.repository.PayoutRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentBlockchainReconciliationWorker unit tests")
class PaymentBlockchainReconciliationWorkerTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PayoutRepository payoutRepository;
    @Mock PaymentBlockchainService blockchainService;
    @InjectMocks PaymentBlockchainReconciliationWorker worker;

    @Test
    @DisplayName("reconcileOnChainState flags confirmed payment missing on-chain receipt")
    void reconcileOnChainState_flagsDrift() {
        Payment payment = Payment.builder()
                .orderId(UUID.randomUUID())
                .payerId(UUID.randomUUID())
                .payerWallet("0xPAYER")
                .amount(java.math.BigDecimal.TEN)
                .currency(com.tokenrealty.payment.entity.PaymentCurrency.USDC)
                .status(PaymentStatus.CONFIRMED)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .idempotencyKey("key-1")
                .txHash("0xREAL123")
                .build();
        payment.setId(UUID.randomUUID());

        when(blockchainService.isEnabled()).thenReturn(true);
        when(paymentRepository.findTop50ByStatusAndTxHashIsNotNullOrderByCreatedAtAsc(PaymentStatus.CONFIRMED))
                .thenReturn(List.of(payment));
        when(payoutRepository.findTop50ByStatusAndTxHashIsNotNullOrderByCreatedAtAsc(
                com.tokenrealty.payment.entity.Payout.PayoutStatus.COMPLETED))
                .thenReturn(List.of());
        when(blockchainService.findReceipt("0xREAL123")).thenReturn(Optional.empty());

        worker.reconcileOnChainState();

        verify(blockchainService).findReceipt("0xREAL123");
    }
}
