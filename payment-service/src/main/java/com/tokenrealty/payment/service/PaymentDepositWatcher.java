package com.tokenrealty.payment.service;

import com.tokenrealty.payment.blockchain.PaymentBlockchainService;
import com.tokenrealty.payment.dto.PaymentDtos.ConfirmPaymentRequest;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

@Component
@ConditionalOnProperty(name = "tokenrealty.payment.blockchain.deposit-watcher.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PaymentDepositWatcher {

    private final PaymentRepository paymentRepository;
    private final PaymentBlockchainService paymentBlockchainService;
    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${tokenrealty.payment.blockchain.deposit-watcher.poll-ms:5000}")
    public void watchPendingDeposits() {
        paymentRepository.findTop50ByStatusAndTxHashIsNotNullOrderByCreatedAtAsc(Payment.PaymentStatus.PENDING)
                .forEach(this::confirmWhenReceiptFound);
    }

    private void confirmWhenReceiptFound(Payment payment) {
        String txHash = payment.getTxHash();
        if (txHash == null || txHash.isBlank() || txHash.startsWith("0xSIMULATED")) {
            return;
        }
        try {
            paymentBlockchainService.findReceipt(txHash).ifPresent(receipt -> confirmDeposit(payment, receipt));
        } catch (Exception ex) {
            log.warn("Deposit watcher failed for payment {}: {}", payment.getId(), ex.getMessage());
        }
    }

    private void confirmDeposit(Payment payment, TransactionReceipt receipt) {
        paymentService.confirm(payment.getId(), new ConfirmPaymentRequest(payment.getTxHash()));
        log.info("Deposit watcher confirmed payment id={} txHash={} block={}",
                payment.getId(), payment.getTxHash(), receipt.getBlockNumber());
    }
}
