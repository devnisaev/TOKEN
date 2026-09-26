package com.tokenrealty.issuance.config;

import com.tokenrealty.issuance.client.PropertyRegistryClient;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevTokenContractInitializer implements ApplicationRunner {

    /** Matches DevPropertyDataInitializer in token-realty-app. */
    public static final UUID DEMO_BUILDING_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID DEMO_FLAT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DEMO_SPV_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final String DEMO_SPV_WALLET = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";
    public static final String DEMO_CONTRACT_ADDRESS = "0xDemoPropertyToken00000000000000000001";

    private static final long DEMO_TOTAL_SUPPLY = 1000L;
    private static final BigDecimal DEMO_TOKEN_PRICE = new BigDecimal("45.00");

    private final TokenContractRepository contractRepository;
    private final TokenHolderRepository holderRepository;
    private final PropertyRegistryClient registryClient;

    @Value("${tokenrealty.issuance.seed-dev-token-contract:true}")
    private boolean seedDevTokenContract;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedDevTokenContract) {
            return;
        }
        if (contractRepository.existsByFlatId(DEMO_FLAT_ID)) {
            return;
        }

        TokenContract contract = contractRepository.save(TokenContract.builder()
                .flatId(DEMO_FLAT_ID)
                .buildingId(DEMO_BUILDING_ID)
                .spvWalletAddress(DEMO_SPV_WALLET)
                .tokenName("Sunrise Flat 101 Token")
                .tokenSymbol("SFT-101")
                .totalSupply(DEMO_TOTAL_SUPPLY)
                .tokenPriceUsd(DEMO_TOKEN_PRICE)
                .contractAddress(DEMO_CONTRACT_ADDRESS)
                .deploymentTxHash("0xDemoDeployTx")
                .deployedAt(Instant.now())
                .network("localhost")
                .chainId(31337L)
                .status(TokenContract.ContractStatus.ACTIVE)
                .build());

        holderRepository.save(TokenHolder.builder()
                .tokenContract(contract)
                .investorId(DEMO_SPV_ID)
                .walletAddress(DEMO_SPV_WALLET)
                .balance(DEMO_TOTAL_SUPPLY)
                .balanceUsd(DEMO_TOKEN_PRICE.multiply(BigDecimal.valueOf(DEMO_TOTAL_SUPPLY)))
                .ownershipPercentage(new BigDecimal("100"))
                .kycVerified(true)
                .whitelistedOnChain(true)
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build());

        try {
            registryClient.setTokenInfo(
                    DEMO_FLAT_ID,
                    DEMO_CONTRACT_ADDRESS,
                    DEMO_TOTAL_SUPPLY,
                    DEMO_TOKEN_PRICE);
            log.info("Seeded demo token contract for flat {} and updated Property Registry", DEMO_FLAT_ID);
        } catch (Exception ex) {
            log.warn("Demo token contract saved locally but Registry token-info update failed: {}", ex.getMessage());
        }
    }
}
