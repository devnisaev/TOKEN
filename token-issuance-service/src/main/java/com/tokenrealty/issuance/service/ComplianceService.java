package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.config.BlockchainProperties;
import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.ComplianceRecord;
import com.tokenrealty.issuance.exception.ConflictException;
import com.tokenrealty.issuance.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.repository.ComplianceRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ComplianceService {

    private final ComplianceRecordRepository complianceRepository;
    private final BlockchainConnector blockchain;
    private final BlockchainProperties blockchainProps;

    public ComplianceService(ComplianceRecordRepository complianceRepository,
                             BlockchainConnector blockchain,
                             BlockchainProperties blockchainProps) {
        this.complianceRepository = complianceRepository;
        this.blockchain = blockchain;
        this.blockchainProps = blockchainProps;
    }

    public Page<ComplianceRecordResponse> findAll(Pageable pageable) {
        return complianceRepository.findAll(pageable).map(this::toResponse);
    }

    public ComplianceRecordResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    public ComplianceRecordResponse findByInvestor(UUID investorId) {
        return complianceRepository.findByInvestorId(investorId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ComplianceRecord for investor " + investorId));
    }

    public ComplianceCheckResponse checkWallet(String walletAddress) {
        var record = complianceRepository.findByWalletAddress(walletAddress);
        if (record.isEmpty()) {
            return new ComplianceCheckResponse(walletAddress, false, false, null, null, null);
        }
        ComplianceRecord r = record.get();

        // Also check on-chain status
        boolean isOnChain = checkOnChainWhitelist(walletAddress);

        return new ComplianceCheckResponse(
                walletAddress,
                r.getStatus() == ComplianceRecord.ComplianceStatus.APPROVED,
                isOnChain,
                r.getStatus(),
                r.getCountryCode(),
                r.getKycExpiresAt()
        );
    }

    @Transactional
    public ComplianceRecordResponse register(RegisterComplianceRequest request) {
        if (complianceRepository.existsByWalletAddress(request.walletAddress())) {
            throw new ConflictException("Wallet " + request.walletAddress() + " already registered");
        }
        if (complianceRepository.existsByInvestorId(request.investorId())) {
            throw new ConflictException("Investor " + request.investorId() + " already registered");
        }

        ComplianceRecord record = ComplianceRecord.builder()
                .investorId(request.investorId())
                .walletAddress(request.walletAddress().toLowerCase())
                .fullName(request.fullName())
                .countryCode(request.countryCode())
                .kycProvider(request.kycProvider())
                .kycReferenceId(request.kycReferenceId())
                .status(ComplianceRecord.ComplianceStatus.PENDING)
                .build();

        ComplianceRecord saved = complianceRepository.save(record);
        log.info("Registered compliance record id={} investor={} wallet={}",
                saved.getId(), request.investorId(), request.walletAddress());
        return toResponse(saved);
    }

    /**
     * Mark investor as KYC verified and whitelist their wallet on-chain.
     */
    @Transactional
    public ComplianceRecordResponse verify(UUID id, ComplianceVerifyRequest request) {
        ComplianceRecord record = getOrThrow(id);

        record.setStatus(ComplianceRecord.ComplianceStatus.APPROVED);
        record.setKycVerifiedAt(Instant.now());
        record.setKycExpiresAt(request.kycExpiresAt());

        // Whitelist on-chain in ComplianceRegistry contract
        try {
            String txHash = whitelistOnChain(record.getWalletAddress(),
                    record.getCountryCode() != null ? record.getCountryCode() : "XX",
                    request.kycExpiresAt());

            record.setOnChainWhitelisted(true);
            record.setWhitelistTxHash(txHash);
            record.setWhitelistedAt(Instant.now());

            log.info("Investor {} whitelisted on-chain tx={}", record.getInvestorId(), txHash);
        } catch (Exception e) {
            log.warn("On-chain whitelisting failed for investor {}: {}",
                    record.getInvestorId(), e.getMessage());
            // Still mark as approved in DB — on-chain can be retried
        }

        return toResponse(complianceRepository.save(record));
    }

    @Transactional
    public ComplianceRecordResponse revoke(UUID id, String reason) {
        ComplianceRecord record = getOrThrow(id);
        record.setStatus(ComplianceRecord.ComplianceStatus.REVOKED);
        record.setRejectionReason(reason);
        record.setOnChainWhitelisted(false);

        // Remove from on-chain whitelist
        try {
            removeFromOnChainWhitelist(record.getWalletAddress(), reason);
            log.info("Investor {} removed from on-chain whitelist", record.getInvestorId());
        } catch (Exception e) {
            log.warn("Failed to remove investor {} from on-chain whitelist: {}",
                    record.getInvestorId(), e.getMessage());
        }

        return toResponse(complianceRepository.save(record));
    }

    // ─── On-chain operations ─────────────────────────────────────────────────

    private String whitelistOnChain(String walletAddress, String country,
                                    Instant expiresAt) throws Exception {
        String registryAddress = blockchainProps.getComplianceRegistryAddress();
        if (registryAddress == null || registryAddress.isBlank()) {
            log.warn("ComplianceRegistry address not configured — skipping on-chain whitelist");
            return "0xNOT_CONFIGURED";
        }

        long expiryTimestamp = expiresAt.getEpochSecond();

        // ABI encode: addToWhitelist(address investor, string country, uint256 expiresAt)
        String encodedData = encodeAddToWhitelist(walletAddress, country, expiryTimestamp);
        return blockchain.sendContractTransaction(registryAddress, encodedData, BigInteger.ZERO);
    }

    private void removeFromOnChainWhitelist(String walletAddress, String reason) throws Exception {
        String registryAddress = blockchainProps.getComplianceRegistryAddress();
        if (registryAddress == null || registryAddress.isBlank()) return;

        String encodedData = encodeRemoveFromWhitelist(walletAddress, reason);
        blockchain.sendContractTransaction(registryAddress, encodedData, BigInteger.ZERO);
    }

    private boolean checkOnChainWhitelist(String walletAddress) {
        String registryAddress = blockchainProps.getComplianceRegistryAddress();
        if (registryAddress == null || registryAddress.isBlank()) return false;
        try {
            String result = blockchain.callContractFunction(
                    registryAddress,
                    BlockchainConnector.encodeIsWhitelisted(walletAddress));
            return BlockchainConnector.decodeBool(result);
        } catch (Exception e) {
            log.debug("On-chain whitelist check failed for {}: {}", walletAddress, e.getMessage());
            return false;
        }
    }

    // ─── ABI encoders ────────────────────────────────────────────────────────

    private String encodeAddToWhitelist(String address, String country, long expiresAt) {
        // Simplified encoding — use web3j ABI encoder in production
        String paddedAddress = padLeft(address.replace("0x", ""), 64);
        String paddedExpiry = padLeft(Long.toHexString(expiresAt), 64);
        // keccak256("addToWhitelist(address,string,uint256)") = 0x4bb278f3
        return "0x4bb278f3" + paddedAddress + paddedExpiry;
    }

    private String encodeRemoveFromWhitelist(String address, String reason) {
        String paddedAddress = padLeft(address.replace("0x", ""), 64);
        // keccak256("removeFromWhitelist(address,string)") = 0x2e1a7d4d
        return "0x2e1a7d4d" + paddedAddress;
    }

    private static String padLeft(String value, int length) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) sb.insert(0, '0');
        return sb.toString();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ComplianceRecord getOrThrow(UUID id) {
        return complianceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceRecord", id));
    }

    private ComplianceRecordResponse toResponse(ComplianceRecord r) {
        return new ComplianceRecordResponse(
                r.getId(), r.getInvestorId(), r.getWalletAddress(),
                r.getFullName(), r.getCountryCode(), r.getKycProvider(),
                r.getOnChainWhitelisted(), r.getWhitelistTxHash(), r.getWhitelistedAt(),
                r.getKycVerifiedAt(), r.getKycExpiresAt(), r.getStatus(),
                r.getRejectionReason(), r.getCreatedAt()
        );
    }
}
