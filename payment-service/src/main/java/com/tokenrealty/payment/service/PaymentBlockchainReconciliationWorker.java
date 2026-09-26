package com.tokenrealty.payment.service;

import com.tokenrealty.payment.blockchain.PaymentBlockchainService;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.repository.PaymentRepository;
import com.tokenrealty.payment.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(name = "tokenrealty.payment.blockchain.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PaymentBlockchainReconciliationWorker {

    private final PaymentRepository paymentRepository;
    private final PayoutRepository payoutRepository;
    private final PaymentBlockchainService blockchainService;

    @Scheduled(cron = "${tokenrealty.payment.reconciliation.cron:0 0 */6 * * *}")
    public void reconcileOnChainState() {
        if (!blockchainService.isEnabled()) {
            return;
        }
        AtomicInteger driftCount = new AtomicInteger();
        paymentRepository.findTop50ByStatusAndTxHashIsNotNullOrderByCreatedAtAsc(Payment.PaymentStatus.CONFIRMED)
                .stream()
                .filter(p -> isRealTxHash(p.getTxHash()))
                .filter(p -> blockchainService.findReceipt(p.getTxHash()).isEmpty())
                .forEach(p -> {
                    driftCount.incrementAndGet();
                    log.warn("Reconciliation drift: payment {} txHash {} not found on-chain",
                            p.getId(), p.getTxHash());
                });

        payoutRepository.findTop50ByStatusAndTxHashIsNotNullOrderByCreatedAtAsc(Payout.PayoutStatus.COMPLETED)
                .stream()
                .filter(p -> isRealTxHash(p.getTxHash()))
                .filter(p -> blockchainService.findReceipt(p.getTxHash()).isEmpty())
                .forEach(p -> {
                    driftCount.incrementAndGet();
                    log.warn("Reconciliation drift: payout {} txHash {} not found on-chain",
                            p.getId(), p.getTxHash());
                });

        if (driftCount.get() > 0) {
            log.warn("Payment reconciliation found {} on-chain drift(s)", driftCount.get());
        } else {
            log.debug("Payment reconciliation complete — no drift detected");
        }
    }

    private static boolean isRealTxHash(String txHash) {
        return txHash != null && !txHash.isBlank() && !txHash.startsWith("0xSIMULATED");
    }
}
