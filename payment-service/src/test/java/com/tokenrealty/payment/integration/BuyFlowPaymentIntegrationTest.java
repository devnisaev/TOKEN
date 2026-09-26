package com.tokenrealty.payment.integration;

import com.tokenrealty.payment.dto.PaymentDtos.ConfirmPaymentRequest;
import com.tokenrealty.payment.dto.PaymentDtos.InitiatePaymentRequest;
import com.tokenrealty.payment.entity.Escrow;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.repository.EscrowRepository;
import com.tokenrealty.payment.repository.PaymentRepository;
import com.tokenrealty.payment.repository.WalletBalanceRepository;
import com.tokenrealty.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Buy flow payment integration test")
class BuyFlowPaymentIntegrationTest {

    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired EscrowRepository escrowRepository;
    @Autowired WalletBalanceRepository walletBalanceRepository;

    @Test
    void initiateAndConfirm_recordsEscrowAndConfirmsPayment() {
        UUID orderId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        String idempotencyKey = "buy-flow-" + orderId;

        var initiated = paymentService.initiate(InitiatePaymentRequest.builder()
                .orderId(orderId)
                .payerId(payerId)
                .payerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .amount(new BigDecimal("100.00"))
                .currency(PaymentCurrency.USDC)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .build(), idempotencyKey);

        Payment payment = paymentRepository.findById(initiated.id()).orElseThrow();
        Escrow escrow = escrowRepository.findByPaymentId(payment.getId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(escrow.getStatus()).isEqualTo(Escrow.EscrowStatus.AWAITING_DEPOSIT);
        assertThat(escrow.getOrderId()).isEqualTo(orderId);

        var confirmed = paymentService.confirm(
                payment.getId(), new ConfirmPaymentRequest("0xBuyFlowPaymentTx"));

        assertThat(confirmed.status()).isEqualTo(Payment.PaymentStatus.CONFIRMED);
        assertThat(confirmed.txHash()).isEqualTo("0xBuyFlowPaymentTx");
        assertThat(escrowRepository.findByPaymentId(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(Escrow.EscrowStatus.HELD);
    }

    @Test
    void releaseEscrow_creditsSellerRecipientBalance() {
        UUID orderId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        String idempotencyKey = "secondary-release-" + orderId;

        var initiated = paymentService.initiate(InitiatePaymentRequest.builder()
                .orderId(orderId)
                .payerId(payerId)
                .payerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .amount(new BigDecimal("250.00"))
                .currency(PaymentCurrency.USDC)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .sellerRecipientId(sellerId)
                .build(), idempotencyKey);

        paymentService.confirm(
                initiated.id(), new ConfirmPaymentRequest("0xSecondaryReleaseTx"));
        paymentService.releaseEscrow(initiated.id());

        var sellerBalance = walletBalanceRepository
                .findByInvestorIdAndCurrency(sellerId, PaymentCurrency.USDC)
                .orElseThrow();
        assertThat(sellerBalance.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("250.00"));
    }
}
