package com.tokenrealty.payment.service;

import com.tokenrealty.payment.config.PaymentProperties;
import com.tokenrealty.payment.dto.PaymentDtos.*;
import com.tokenrealty.payment.entity.*;
import com.tokenrealty.payment.exception.ValidationException;
import com.tokenrealty.payment.kafka.port.PaymentConfirmedPublisher;
import com.tokenrealty.payment.mapper.PaymentMapper;
import com.tokenrealty.payment.repository.EscrowRepository;
import com.tokenrealty.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService unit tests")
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock EscrowRepository escrowRepository;
    @Mock PaymentMapper mapper;
    @Mock PaymentProperties paymentProperties;
    @Mock LedgerService ledgerService;
    @Mock PaymentConfirmedPublisher paymentConfirmedPublisher;
    @InjectMocks PaymentService paymentService;

    private UUID orderId;
    private InitiatePaymentRequest request;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        request = InitiatePaymentRequest.builder()
                .orderId(orderId)
                .payerId(UUID.randomUUID())
                .payerWallet("0xPayer")
                .amount(new BigDecimal("100.00"))
                .currency(PaymentCurrency.USDC)
                .build();
    }

    @Test
    @DisplayName("initiate creates payment and escrow")
    void initiateSuccess() {
        when(paymentProperties.getEscrowWalletAddress()).thenReturn("0xEscrow");
        Payment saved = Payment.builder()
                .orderId(orderId)
                .payerId(request.payerId())
                .payerWallet(request.payerWallet())
                .amount(request.amount())
                .currency(PaymentCurrency.USDC)
                .status(Payment.PaymentStatus.PENDING)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .idempotencyKey("key-1")
                .build();
        saved.setId(UUID.randomUUID());

        Escrow escrow = Escrow.builder()
                .paymentId(saved.getId())
                .orderId(orderId)
                .amount(request.amount())
                .currency(PaymentCurrency.USDC)
                .status(Escrow.EscrowStatus.AWAITING_DEPOSIT)
                .escrowWalletAddress("0xEscrow")
                .build();

        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(saved);
        when(escrowRepository.save(any())).thenReturn(escrow);
        when(mapper.toPaymentResponse(saved, escrow)).thenReturn(
                PaymentResponse.builder().id(saved.getId()).orderId(orderId).build());

        PaymentResponse response = paymentService.initiate(request, "key-1");

        assertThat(response.orderId()).isEqualTo(orderId);
        verify(ledgerService).recordEscrowHold(saved.getId(), request.amount(), PaymentCurrency.USDC);
    }

    @Test
    @DisplayName("confirm rejects non-pending payment")
    void confirmNotPending() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder().status(Payment.PaymentStatus.CONFIRMED).build();
        payment.setId(paymentId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirm(paymentId,
                ConfirmPaymentRequest.builder().txHash("0xabc").build()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("initiate requires idempotency key")
    void requiresIdempotencyKey() {
        assertThatThrownBy(() -> paymentService.initiate(request, " "))
                .isInstanceOf(ValidationException.class);
    }
}
