package com.tokenrealty.payment.service;

import com.tokenrealty.payment.blockchain.PaymentBlockchainService;
import com.tokenrealty.payment.config.PaymentProperties;
import com.tokenrealty.payment.dto.PaymentDtos.*;
import com.tokenrealty.payment.entity.*;
import com.tokenrealty.web.exception.IdempotencyConflictException;
import com.tokenrealty.web.exception.InsufficientFundsException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.payment.kafka.events.PaymentConfirmedEvent;
import com.tokenrealty.payment.kafka.port.PaymentConfirmedPublisher;
import com.tokenrealty.payment.mapper.PaymentMapper;
import com.tokenrealty.payment.repository.EscrowRepository;
import com.tokenrealty.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EscrowRepository escrowRepository;
    private final PaymentMapper mapper;
    private final PaymentProperties paymentProperties;
    private final LedgerService ledgerService;
    private final PaymentConfirmedPublisher paymentConfirmedPublisher;
    private final PaymentBlockchainService paymentBlockchainService;
    private final WalletBalanceService walletBalanceService;

    public Page<PaymentResponse> findAll(UUID payerId, UUID orderId, Pageable pageable) {
        if (payerId != null) {
            return mapPage(paymentRepository.findByPayerId(payerId, pageable));
        }
        if (orderId != null) {
            return mapPage(paymentRepository.findByOrderId(orderId, pageable));
        }
        return mapPage(paymentRepository.findAll(pageable));
    }

    public PaymentResponse findById(UUID id) {
        Payment payment = getPayment(id);
        Escrow escrow = escrowRepository.findByPaymentId(id).orElse(null);
        return mapper.toPaymentResponse(payment, escrow);
    }

    @Transactional
    public PaymentResponse initiate(InitiatePaymentRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        var existing = findIdempotent(request, idempotencyKey);
        if (existing != null) {
            return existing;
        }

        Payment payment = paymentRepository.save(buildPendingPayment(request, idempotencyKey));
        Escrow escrow = escrowRepository.save(buildEscrow(payment, request));
        ledgerService.recordEscrowHold(payment.getId(), payment.getAmount(), payment.getCurrency());
        walletBalanceService.holdForPayment(request.payerId(), request.amount(), request.currency());
        return mapper.toPaymentResponse(payment, escrow);
    }

    @Transactional
    public PaymentResponse confirm(UUID paymentId, ConfirmPaymentRequest request) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            raiseValidation("Payment is not pending confirmation");
        }
        if (paymentBlockchainService.isEnabled()
                && paymentBlockchainService.findReceipt(request.txHash()).isEmpty()) {
            raiseValidation("On-chain transaction not confirmed: " + request.txHash());
        }
        Escrow escrow = getEscrow(paymentId);
        payment.setStatus(Payment.PaymentStatus.CONFIRMED);
        payment.setTxHash(request.txHash());
        payment.setConfirmedAt(Instant.now());
        escrow.setStatus(Escrow.EscrowStatus.HELD);
        walletBalanceService.settleConfirmedPayment(
                payment.getPayerId(), payment.getAmount(), payment.getCurrency());
        paymentConfirmedPublisher.publishPaymentConfirmed(new PaymentConfirmedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getPayerId(),
                new PaymentConfirmedEvent.Amount(
                        payment.getAmount().toPlainString(), payment.getCurrency()),
                payment.getTxHash(),
                payment.getConfirmedAt()));
        return mapper.toPaymentResponse(payment, escrow);
    }

    @Transactional
    public PaymentResponse releaseEscrow(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        if (payment.getStatus() != Payment.PaymentStatus.CONFIRMED) {
            raiseValidation("Payment must be confirmed before escrow release");
        }
        Escrow escrow = getEscrow(paymentId);
        if (escrow.getStatus() != Escrow.EscrowStatus.HELD) {
            raiseValidation("Escrow is not held");
        }
        escrow.setStatus(Escrow.EscrowStatus.RELEASED);
        payment.setStatus(Payment.PaymentStatus.RELEASED);
        ledgerService.recordEscrowRelease(payment.getId(), payment.getAmount(), payment.getCurrency());
        if (payment.getSellerRecipientId() != null) {
            walletBalanceService.credit(
                    payment.getSellerRecipientId(), payment.getAmount(), payment.getCurrency());
        }
        return mapper.toPaymentResponse(payment, escrow);
    }

    @Transactional
    public PaymentResponse refund(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        Escrow escrow = getEscrow(paymentId);
        if (escrow.getStatus() == Escrow.EscrowStatus.RELEASED) {
            raiseValidation("Escrow already released");
        }
        escrow.setStatus(Escrow.EscrowStatus.REFUNDED);
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        walletBalanceService.releaseHold(payment.getPayerId(), payment.getAmount(), payment.getCurrency());
        return mapper.toPaymentResponse(payment, escrow);
    }

    private Page<PaymentResponse> mapPage(Page<Payment> page) {
        return page.map(p -> mapper.toPaymentResponse(p,
                escrowRepository.findByPaymentId(p.getId()).orElse(null)));
    }

    private PaymentResponse findIdempotent(InitiatePaymentRequest request, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    if (!matches(existing, request)) {
                        throw new IdempotencyConflictException(
                                "Idempotency key reused with different payload");
                    }
                    Escrow escrow = escrowRepository.findByPaymentId(existing.getId()).orElse(null);
                    return mapper.toPaymentResponse(existing, escrow);
                })
                .orElse(null);
    }

    private static boolean matches(Payment existing, InitiatePaymentRequest request) {
        return existing.getOrderId().equals(request.orderId())
                && existing.getPayerId().equals(request.payerId())
                && existing.getAmount().compareTo(request.amount()) == 0
                && existing.getCurrency() == request.currency();
    }

    private Payment buildPendingPayment(InitiatePaymentRequest request, String idempotencyKey) {
        return Payment.builder()
                .orderId(request.orderId())
                .payerId(request.payerId())
                .sellerRecipientId(request.sellerRecipientId())
                .payerWallet(request.payerWallet())
                .amount(request.amount())
                .currency(request.currency())
                .paymentType(request.paymentType())
                .status(Payment.PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private Escrow buildEscrow(Payment payment, InitiatePaymentRequest request) {
        return Escrow.builder()
                .paymentId(payment.getId())
                .orderId(request.orderId())
                .amount(request.amount())
                .currency(request.currency())
                .status(Escrow.EscrowStatus.AWAITING_DEPOSIT)
                .escrowWalletAddress(paymentProperties.getEscrowWalletAddress())
                .build();
    }

    private Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    private Escrow getEscrow(UUID paymentId) {
        return escrowRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow not found for payment: " + paymentId));
    }

    private static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            raiseValidation("Idempotency-Key header is required");
        }
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
