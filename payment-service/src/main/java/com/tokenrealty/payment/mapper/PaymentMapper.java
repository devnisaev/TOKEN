package com.tokenrealty.payment.mapper;

import com.tokenrealty.payment.dto.PaymentDtos.*;
import com.tokenrealty.payment.entity.Escrow;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.entity.WalletBalance;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(Payment payment, Escrow escrow) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .payerId(payment.getPayerId())
                .payerWallet(payment.getPayerWallet())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentType(payment.getPaymentType())
                .txHash(payment.getTxHash())
                .confirmedAt(payment.getConfirmedAt())
                .escrow(escrow != null ? toEscrowResponse(escrow) : null)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public EscrowResponse toEscrowResponse(Escrow escrow) {
        return EscrowResponse.builder()
                .id(escrow.getId())
                .paymentId(escrow.getPaymentId())
                .orderId(escrow.getOrderId())
                .amount(escrow.getAmount())
                .currency(escrow.getCurrency())
                .status(escrow.getStatus())
                .escrowWalletAddress(escrow.getEscrowWalletAddress())
                .build();
    }

    public PayoutResponse toPayoutResponse(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .recipientInvestorId(payout.getRecipientInvestorId())
                .recipientWallet(payout.getRecipientWallet())
                .amount(payout.getAmount())
                .currency(payout.getCurrency())
                .purpose(payout.getPurpose())
                .referenceId(payout.getReferenceId())
                .period(payout.getPeriod())
                .status(payout.getStatus())
                .txHash(payout.getTxHash())
                .completedAt(payout.getCompletedAt())
                .createdAt(payout.getCreatedAt())
                .build();
    }

    public WalletBalanceResponse toWalletBalanceResponse(WalletBalance balance) {
        return WalletBalanceResponse.builder()
                .investorId(balance.getInvestorId())
                .currency(balance.getCurrency())
                .availableBalance(balance.getAvailableBalance())
                .heldBalance(balance.getHeldBalance())
                .build();
    }
}
