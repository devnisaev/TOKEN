package com.tokenrealty.payment.service;

import com.tokenrealty.payment.dto.PaymentDtos.CreatePayoutRequest;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.kafka.command.DividendDistributedCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DividendPayoutService {

    private final PayoutService payoutService;
    private final TaxWithholdingService taxWithholdingService;

    @Transactional
    public void createHolderPayouts(DividendDistributedCommand command) {
        for (DividendDistributedCommand.HolderPayout holder : command.holderPayouts()) {
            BigDecimal gross = holder.amount();
            BigDecimal withholding = taxWithholdingService.calculateWithholding(gross);
            BigDecimal net = gross.subtract(withholding);
            payoutService.create(new CreatePayoutRequest(
                    holder.investorId(),
                    holder.walletAddress(),
                    net,
                    PaymentCurrency.USDC,
                    Payout.PayoutPurpose.DIVIDEND,
                    command.contractId(),
                    command.flatId(),
                    null,
                    command.period(),
                    holder.dividendPaymentId(),
                    gross,
                    withholding));
        }
    }
}
