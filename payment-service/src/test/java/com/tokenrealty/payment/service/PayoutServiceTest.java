package com.tokenrealty.payment.service;

import com.tokenrealty.payment.blockchain.PaymentBlockchainService;
import com.tokenrealty.payment.dto.PaymentDtos.*;
import com.tokenrealty.payment.entity.*;
import com.tokenrealty.payment.kafka.port.PayoutCompletedPublisher;
import com.tokenrealty.payment.kafka.port.RentCollectedPublisher;
import com.tokenrealty.payment.mapper.PaymentMapper;
import com.tokenrealty.payment.repository.PayoutRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayoutService unit tests")
class PayoutServiceTest {

    @Mock PayoutRepository payoutRepository;
    @Mock PaymentMapper mapper;
    @Mock RentCollectedPublisher rentCollectedPublisher;
    @Mock PayoutCompletedPublisher payoutCompletedPublisher;
    @Mock PaymentBlockchainService paymentBlockchainService;
    @InjectMocks PayoutService payoutService;

    @Test
    @DisplayName("create payout completes and publishes rent event")
    void createRentPayout() {
        CreatePayoutRequest request = CreatePayoutRequest.builder()
                .recipientInvestorId(UUID.randomUUID())
                .recipientWallet("0xRecipient")
                .amount(new BigDecimal("50.00"))
                .currency(PaymentCurrency.USDC)
                .purpose(Payout.PayoutPurpose.RENT)
                .referenceId(UUID.randomUUID())
                .period("2025-09")
                .build();

        Payout saved = Payout.builder()
                .recipientInvestorId(request.recipientInvestorId())
                .amount(request.amount())
                .currency(request.currency())
                .referenceId(request.referenceId())
                .period(request.period())
                .purpose(Payout.PayoutPurpose.RENT)
                .status(Payout.PayoutStatus.COMPLETED)
                .build();
        saved.setId(UUID.randomUUID());

        when(paymentBlockchainService.isEnabled()).thenReturn(false);
        when(payoutRepository.save(any())).thenReturn(saved);
        when(mapper.toPayoutResponse(saved)).thenReturn(
                PayoutResponse.builder().id(saved.getId()).status(Payout.PayoutStatus.COMPLETED).build());

        PayoutResponse response = payoutService.create(request);

        assertThat(response.status()).isEqualTo(Payout.PayoutStatus.COMPLETED);
        verify(rentCollectedPublisher).publishRentCollected(any());
    }

    @Test
    @DisplayName("create dividend payout publishes payout completed event")
    void createDividendPayout() {
        UUID dividendPaymentId = UUID.randomUUID();
        CreatePayoutRequest request = CreatePayoutRequest.builder()
                .recipientInvestorId(UUID.randomUUID())
                .recipientWallet("0xRecipient")
                .amount(new BigDecimal("25.00"))
                .currency(PaymentCurrency.USDC)
                .purpose(Payout.PayoutPurpose.DIVIDEND)
                .referenceId(UUID.randomUUID())
                .dividendPaymentId(dividendPaymentId)
                .period("2025-09")
                .build();

        Payout saved = Payout.builder()
                .recipientInvestorId(request.recipientInvestorId())
                .amount(request.amount())
                .currency(request.currency())
                .referenceId(request.referenceId())
                .dividendPaymentId(dividendPaymentId)
                .purpose(Payout.PayoutPurpose.DIVIDEND)
                .status(Payout.PayoutStatus.COMPLETED)
                .build();
        saved.setId(UUID.randomUUID());

        when(paymentBlockchainService.isEnabled()).thenReturn(false);
        when(payoutRepository.save(any())).thenReturn(saved);
        when(mapper.toPayoutResponse(saved)).thenReturn(
                PayoutResponse.builder().id(saved.getId()).status(Payout.PayoutStatus.COMPLETED).build());

        payoutService.create(request);

        verify(payoutCompletedPublisher).publishPayoutCompleted(any());
    }
}
