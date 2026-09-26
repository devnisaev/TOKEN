package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
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
            String encodedData = encodeAddToWhitelist(walletAddress, country, expiresAt.getEpochSecond());
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
            String encodedData = encodeRemoveFromWhitelist(walletAddress, reason);
            blockchain.sendContractTransaction(registryAddress, encodedData, BigInteger.ZERO);
        } catch (Exception ex) {
            throw new IllegalStateException("On-chain revoke failed", ex);
        }
    }

    private static String encodeAddToWhitelist(String address, String country, long expiresAt) {
        String paddedAddress = padLeft(address.replace("0x", ""), 64);
        String paddedExpiry = padLeft(Long.toHexString(expiresAt), 64);
        return "0x4bb278f3" + paddedAddress + paddedExpiry;
    }

    private static String encodeRemoveFromWhitelist(String address, String reason) {
        String paddedAddress = padLeft(address.replace("0x", ""), 64);
        return "0x2e1a7d4d" + paddedAddress;
    }

    private static String padLeft(String value, int length) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
}
