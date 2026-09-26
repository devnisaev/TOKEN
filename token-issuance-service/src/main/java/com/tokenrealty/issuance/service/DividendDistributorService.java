package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.blockchain.encode.DividendDistributorEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Optional on-chain deposit into {@code DividendDistributor} when configured on the token contract.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DividendDistributorService {

    private final BlockchainConnector blockchain;

    public Optional<String> depositIfConfigured(String distributorAddress, BigDecimal amountUsd) {
        if (distributorAddress == null || distributorAddress.isBlank()) {
            return Optional.empty();
        }
        try {
            BigDecimal normalized = amountUsd.setScale(2, java.math.RoundingMode.HALF_UP);
            var tokenUnits = toUsdcUnits(normalized);
            String encoded = DividendDistributorEncoder.encodeDeposit(tokenUnits);
            String txHash = blockchain.sendContractTransaction(distributorAddress, encoded, java.math.BigInteger.ZERO);
            log.info("Deposited {} USDC to DividendDistributor {} tx={}", normalized, distributorAddress, txHash);
            return Optional.of(txHash);
        } catch (Exception ex) {
            log.warn("DividendDistributor deposit skipped for {}: {}", distributorAddress, ex.getMessage());
            return Optional.empty();
        }
    }

    private static BigInteger toUsdcUnits(BigDecimal amount) {
        return amount.movePointRight(6).setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }
}
