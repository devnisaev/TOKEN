package com.tokenrealty.payment.integration;

import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.kafka.command.DividendDistributedCommand;
import com.tokenrealty.payment.repository.PayoutRepository;
import com.tokenrealty.payment.service.DividendPayoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "tokenrealty.payment.tax.withholding-rate=0.15")
class DividendWithholdingIntegrationTest {

    @Autowired DividendPayoutService dividendPayoutService;
    @Autowired PayoutRepository payoutRepository;

    @Test
    @DisplayName("dividend payout applies configured withholding rate")
    void dividendPayout_appliesWithholding() {
        UUID investorId = UUID.randomUUID();
        UUID dividendPaymentId = UUID.randomUUID();

        dividendPayoutService.createHolderPayouts(new DividendDistributedCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "2025-09",
                List.of(new DividendDistributedCommand.HolderPayout(
                        dividendPaymentId,
                        investorId,
                        "0xInvestorWallet",
                        new BigDecimal("100.00")))));

        Payout payout = payoutRepository.findAll().getFirst();
        assertThat(payout.getGrossAmountUsd()).isEqualByComparingTo("100.00");
        assertThat(payout.getWithholdingAmountUsd()).isEqualByComparingTo("15.00");
        assertThat(payout.getAmount()).isEqualByComparingTo("85.00");
        assertThat(payout.getCurrency()).isEqualTo(PaymentCurrency.USDC);
    }
}
