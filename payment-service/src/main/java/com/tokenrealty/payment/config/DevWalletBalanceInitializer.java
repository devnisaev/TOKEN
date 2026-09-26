package com.tokenrealty.payment.config;

import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.service.WalletBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevWalletBalanceInitializer implements ApplicationRunner {

    /** Matches Hardhat account #1 — see docs/hardhat-demo.md */
    public static final UUID DEMO_INVESTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final WalletBalanceService walletBalanceService;

    @Value("${tokenrealty.payment.seed-dev-balances:true}")
    private boolean seedDevBalances;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedDevBalances) {
            return;
        }
        var balance = walletBalanceService.getByInvestorId(DEMO_INVESTOR_ID);
        if (balance.availableBalance().compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        walletBalanceService.credit(
                DEMO_INVESTOR_ID,
                new BigDecimal("10000.00"),
                PaymentCurrency.USDC);
        log.info("Seeded demo investor USDC balance for {}", DEMO_INVESTOR_ID);
    }
}
