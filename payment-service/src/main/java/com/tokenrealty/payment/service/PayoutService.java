package com.tokenrealty.payment.service;

import com.tokenrealty.payment.dto.PaymentDtos.*;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.kafka.PaymentEventPublisher;
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
    private final PaymentEventPublisher eventPublisher;

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
                .period(request.period())
                .status(Payout.PayoutStatus.PENDING)
                .build();

        Payout saved = payoutRepository.save(payout);
        completePayout(saved);
        if (saved.getPurpose() == Payout.PayoutPurpose.RENT) {
            eventPublisher.publishRentCollected(saved);
        }
        return mapper.toPayoutResponse(saved);
    }

    private void completePayout(Payout payout) {
        payout.setStatus(Payout.PayoutStatus.COMPLETED);
        payout.setCompletedAt(Instant.now());
        payout.setTxHash("0xSIMULATED_" + payout.getId().toString().replace("-", "").substring(0, 16));
    }
}
