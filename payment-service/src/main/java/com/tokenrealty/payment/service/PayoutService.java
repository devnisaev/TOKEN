package com.tokenrealty.payment.service;

import com.tokenrealty.payment.blockchain.PaymentBlockchainService;
import com.tokenrealty.payment.dto.PaymentDtos.*;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.kafka.events.RentCollectedEvent;
import com.tokenrealty.payment.kafka.port.RentCollectedPublisher;
import com.tokenrealty.payment.mapper.PaymentMapper;
import com.tokenrealty.payment.repository.PayoutRepository;
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
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final PaymentMapper mapper;
    private final RentCollectedPublisher rentCollectedPublisher;
    private final PaymentBlockchainService paymentBlockchainService;

    public Page<PayoutResponse> findAll(UUID recipientInvestorId, Pageable pageable) {
        if (recipientInvestorId != null) {
            return payoutRepository.findByRecipientInvestorId(recipientInvestorId, pageable)
                    .map(mapper::toPayoutResponse);
        }
        return payoutRepository.findAll(pageable).map(mapper::toPayoutResponse);
    }

    @Transactional
    public PayoutResponse create(CreatePayoutRequest request) {
        Payout payout = Payout.builder()
                .recipientInvestorId(request.recipientInvestorId())
                .recipientWallet(request.recipientWallet())
                .amount(request.amount())
                .currency(request.currency())
                .purpose(request.purpose())
                .referenceId(request.referenceId())
                .flatId(request.flatId())
                .tenantId(request.tenantId())
                .period(request.period())
                .status(Payout.PayoutStatus.PENDING)
                .build();

        Payout saved = payoutRepository.save(payout);
        completePayout(saved);
        if (saved.getPurpose() == Payout.PayoutPurpose.RENT) {
            UUID tenantId = saved.getTenantId() != null ? saved.getTenantId() : saved.getRecipientInvestorId();
            rentCollectedPublisher.publishRentCollected(new RentCollectedEvent(
                    saved.getId(),
                    saved.getReferenceId(),
                    saved.getFlatId(),
                    tenantId,
                    saved.getPeriod(),
                    new RentCollectedEvent.Amount(
                            saved.getAmount().toPlainString(), saved.getCurrency()),
                    saved.getTxHash()));
        }
        return mapper.toPayoutResponse(saved);
    }

    private void completePayout(Payout payout) {
        payout.setStatus(Payout.PayoutStatus.COMPLETED);
        payout.setCompletedAt(Instant.now());
        if (paymentBlockchainService.isEnabled()) {
            payout.setTxHash(paymentBlockchainService.sendTokenTransfer(
                    payout.getRecipientWallet(), payout.getAmount(), payout.getCurrency()));
        } else {
            payout.setTxHash("0xSIMULATED_" + payout.getId().toString().replace("-", "").substring(0, 16));
        }
    }
}
