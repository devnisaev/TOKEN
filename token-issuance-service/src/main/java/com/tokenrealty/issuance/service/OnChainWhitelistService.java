package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.blockchain.encode.ComplianceRegistryEncoder;
import com.tokenrealty.issuance.config.BlockchainProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnChainWhitelistService {

    private final BlockchainConnector blockchain;
    private final BlockchainProperties blockchainProps;

    public String whitelist(String walletAddress, String country, Instant expiresAt) {
        String registryAddress = blockchainProps.getComplianceRegistryAddress();
        if (registryAddress == null || registryAddress.isBlank()) {
            log.warn("ComplianceRegistry address not configured — skipping on-chain whitelist");
            return "0xNOT_CONFIGURED";
        }
        try {
            String encodedData = ComplianceRegistryEncoder.encodeAddToWhitelist(
                    walletAddress, country, expiresAt.getEpochSecond());
            return blockchain.sendContractTransaction(registryAddress, encodedData, BigInteger.ZERO);
        } catch (Exception ex) {
            throw new IllegalStateException("On-chain whitelist failed", ex);
        }
    }

    public void removeFromWhitelist(String walletAddress, String reason) {
        String registryAddress = blockchainProps.getComplianceRegistryAddress();
        if (registryAddress == null || registryAddress.isBlank()) {
            return;
        }
        try {
            String encodedData = ComplianceRegistryEncoder.encodeRemoveFromWhitelist(walletAddress);
            blockchain.sendContractTransaction(registryAddress, encodedData, BigInteger.ZERO);
            log.info("On-chain whitelist removed for {} reason={}", walletAddress, reason);
        } catch (Exception ex) {
            throw new IllegalStateException("On-chain revoke failed", ex);
        }
    }
}
