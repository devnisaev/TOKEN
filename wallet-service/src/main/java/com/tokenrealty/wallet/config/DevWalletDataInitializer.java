package com.tokenrealty.wallet.config;

import com.tokenrealty.wallet.entity.InvestorWallet;
import com.tokenrealty.wallet.entity.InvestorWallet.WalletType;
import com.tokenrealty.wallet.repository.InvestorWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevWalletDataInitializer implements ApplicationRunner {

    /** Matches DevComplianceDataInitializer / Auth demo investor. */
    public static final UUID DEMO_INVESTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final String DEMO_INVESTOR_WALLET = "0x70997970c51812dc3a010c724d1afe6fc599aa84";

    private final InvestorWalletRepository repository;

    @Value("${tokenrealty.wallet.seed-dev-wallets:true}")
    private boolean seedDevWallets;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedDevWallets) {
            return;
        }
        if (repository.existsByInvestorIdAndWalletType(DEMO_INVESTOR_ID, WalletType.LINKED)) {
            return;
        }
        if (repository.existsByWalletAddressIgnoreCase(DEMO_INVESTOR_WALLET)) {
            return;
        }

        repository.save(InvestorWallet.builder()
                .investorId(DEMO_INVESTOR_ID)
                .walletAddress(DEMO_INVESTOR_WALLET)
                .walletType(WalletType.LINKED)
                .label("Hardhat account #1 (demo)")
                .primary(true)
                .build());
        log.info("Seeded linked demo wallet for investor {}", DEMO_INVESTOR_ID);
    }
}
