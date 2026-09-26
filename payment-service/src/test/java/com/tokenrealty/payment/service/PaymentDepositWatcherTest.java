package com.tokenrealty.payment.service;

import com.tokenrealty.payment.blockchain.PaymentBlockchainService;
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
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentDepositWatcher unit tests")
class PaymentDepositWatcherTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentBlockchainService paymentBlockchainService;
    @Mock PaymentService paymentService;
    @InjectMocks PaymentDepositWatcher watcher;

    @Test
    void watchPendingDeposits_confirmsWhenReceiptFound() {
        UUID paymentId = UUID.randomUUID();
        String txHash = "0xDepositTxHash123456789012345678901234567890123456789012345678901234";
        Payment pending = Payment.builder()
                .orderId(UUID.randomUUID())
                .payerId(UUID.randomUUID())
                .payerWallet("0xBuyer")
                .amount(java.math.BigDecimal.TEN)
                .currency(PaymentCurrency.USDC)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .status(Payment.PaymentStatus.PENDING)
                .txHash(txHash)
                .idempotencyKey("key-" + paymentId)
                .build();
        pending.setId(paymentId);

        TransactionReceipt receipt = mock(TransactionReceipt.class);

        when(paymentRepository.findTop50ByStatusAndTxHashIsNotNullOrderByCreatedAtAsc(Payment.PaymentStatus.PENDING))
                .thenReturn(List.of(pending));
        when(paymentBlockchainService.findReceipt(txHash)).thenReturn(Optional.of(receipt));

        watcher.watchPendingDeposits();

        verify(paymentService).confirm(eq(paymentId), any(ConfirmPaymentRequest.class));
    }

    @Test
    void watchPendingDeposits_skipsWhenReceiptMissing() {
        UUID paymentId = UUID.randomUUID();
        String txHash = "0xPendingDepositTxHash1234567890123456789012345678901234567890123456";
        Payment pending = Payment.builder()
                .orderId(UUID.randomUUID())
                .payerId(UUID.randomUUID())
                .payerWallet("0xBuyer")
                .amount(java.math.BigDecimal.TEN)
                .currency(PaymentCurrency.USDC)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .status(Payment.PaymentStatus.PENDING)
                .txHash(txHash)
                .idempotencyKey("key-" + paymentId)
                .build();
        pending.setId(paymentId);

        when(paymentRepository.findTop50ByStatusAndTxHashIsNotNullOrderByCreatedAtAsc(Payment.PaymentStatus.PENDING))
                .thenReturn(List.of(pending));
        when(paymentBlockchainService.findReceipt(txHash)).thenReturn(Optional.empty());

        watcher.watchPendingDeposits();

        verify(paymentService, never()).confirm(any(), any());
    }
}
