package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.config.BlockchainProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

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
            String encodedData = encodeRemoveFromWhitelist(walletAddress);
            blockchain.sendContractTransaction(registryAddress, encodedData, BigInteger.ZERO);
            log.info("On-chain whitelist removed for {} reason={}", walletAddress, reason);
        } catch (Exception ex) {
            throw new IllegalStateException("On-chain revoke failed", ex);
        }
    }

    private static String encodeAddToWhitelist(String address, String country, long expiresAt) {
        Function function = new Function(
                "addToWhitelist",
                List.of(
                        new Address(address),
                        new Utf8String(country != null ? country : ""),
                        new Uint256(BigInteger.valueOf(expiresAt))),
                Collections.emptyList());
        return FunctionEncoder.encode(function);
    }

    private static String encodeRemoveFromWhitelist(String address) {
        String paddedAddress = padLeft(address.replace("0x", ""), 64);
        return "0x8ab1d681" + paddedAddress;
    }

    private static String padLeft(String value, int length) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
}
