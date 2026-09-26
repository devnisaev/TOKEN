package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.entity.TokenTransfer;
import com.tokenrealty.issuance.exception.ComplianceException;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class TransferService {

    private final TokenContractRepository contractRepository;
    private final TokenHolderRepository holderRepository;
    private final TokenTransferRepository transferRepository;
    private final com.tokenrealty.issuance.client.ComplianceClient complianceClient;
    private final BlockchainConnector blockchain;

    public TransferService(TokenContractRepository contractRepository,
                           TokenHolderRepository holderRepository,
                           TokenTransferRepository transferRepository,
                           com.tokenrealty.issuance.client.ComplianceClient complianceClient,
                           BlockchainConnector blockchain) {
        this.contractRepository = contractRepository;
        this.holderRepository = holderRepository;
        this.transferRepository = transferRepository;
        this.complianceClient = complianceClient;
        this.blockchain = blockchain;
    }

    public Page<TokenTransferResponse> findByContract(UUID contractId, Pageable pageable) {
        return transferRepository.findByTokenContractId(contractId, pageable)
                .map(this::toResponse);
    }

    public Page<TokenTransferResponse> findByWallet(String walletAddress, Pageable pageable) {
        return transferRepository.findByToAddress(walletAddress, pageable)
                .map(this::toResponse);
    }

    public TokenTransferResponse findById(UUID id) {
        return toResponse(transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TokenTransfer", id)));
    }

    /**
     * Execute a token transfer between two investors.
     * Enforces compliance checks before submitting on-chain.
     */
    @Transactional
    public TokenTransferResponse transfer(UUID contractId, TransferRequest request) {
        TokenContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("TokenContract", contractId));

        // 1. Contract must be ACTIVE
        if (contract.getStatus() != TokenContract.ContractStatus.ACTIVE) {
            throw new ConflictException("Contract is not ACTIVE — status: " + contract.getStatus());
        }

        // 2. Compliance check — both wallets must be KYC whitelisted
        checkCompliance(request.fromAddress(), "sender");
        checkCompliance(request.toAddress(), "recipient");

        // 3. Find sender's holder record
        TokenHolder sender = holderRepository
                .findByTokenContractIdAndWalletAddress(contractId, request.fromAddress())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Holder not found: wallet=" + request.fromAddress()));

        // 4. Check sender balance
        if (sender.getBalance() < request.amount()) {
            throw new ConflictException(
                    "Insufficient balance: has=" + sender.getBalance() + " requested=" + request.amount());
        }

        // 5. Create transfer record
        BigDecimal pricePerToken = request.pricePerTokenUsd() != null
                ? request.pricePerTokenUsd()
                : contract.getTokenPriceUsd();

        BigDecimal totalValue = pricePerToken.multiply(BigDecimal.valueOf(request.amount()));

        TokenTransfer transfer = TokenTransfer.builder()
                .tokenContract(contract)
                .fromAddress(request.fromAddress())
                .toAddress(request.toAddress())
                .amount(request.amount())
                .pricePerTokenUsd(pricePerToken)
                .totalValueUsd(totalValue)
                .type(TokenTransfer.TransferType.TRANSFER)
                .status(TokenTransfer.TransferStatus.PENDING)
                .build();
        transfer = transferRepository.save(transfer);

        // 6. Submit on-chain transaction
        try {
            // ABI encode: transfer(address to, uint256 amount)
            String encodedData = encodeTransfer(request.toAddress(), request.amount());
            String txHash = blockchain.sendContractTransaction(
                    contract.getContractAddress(), encodedData, BigInteger.ZERO);

            transfer.setTxHash(txHash);
            transfer.setStatus(TokenTransfer.TransferStatus.PENDING);
            transferRepository.save(transfer);

            // 7. Wait for confirmation and update local state
            var receipt = blockchain.waitForReceipt(txHash);
            if (blockchain.isTransactionSuccessful(receipt)) {
                transfer.setStatus(TokenTransfer.TransferStatus.CONFIRMED);
                transfer.setBlockNumber(receipt.getBlockNumber().longValue());
                transfer.setConfirmedAt(Instant.now());
                transferRepository.save(transfer);

                // Update holder balances in DB (mirrors on-chain state)
                updateHolderBalance(contract, sender, sender.getBalance() - request.amount());
                updateOrCreateRecipientHolder(contractId, contract, request.toAddress(), request.amount(),
                        contract.getTokenPriceUsd());

                log.info("Transfer confirmed: {} tokens from {} to {} tx={}",
                        request.amount(), request.fromAddress(), request.toAddress(), txHash);
            } else {
                transfer.setStatus(TokenTransfer.TransferStatus.FAILED);
                transfer.setFailureReason("Transaction reverted on-chain");
                transferRepository.save(transfer);
                throw new RuntimeException("Transfer transaction reverted");
            }

        } catch (Exception e) {
            if (transfer.getStatus() == TokenTransfer.TransferStatus.PENDING) {
                transfer.setStatus(TokenTransfer.TransferStatus.FAILED);
                transfer.setFailureReason(e.getMessage());
                transferRepository.save(transfer);
            }
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }

        return toResponse(transfer);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void checkCompliance(String walletAddress, String role) {
        if (!complianceClient.isWalletApproved(walletAddress)) {
            throw new ComplianceException(role + " wallet " + walletAddress + " is not KYC approved");
        }
    }

    private void updateHolderBalance(TokenContract contract, TokenHolder holder, long newBalance) {
        BigDecimal ownershipPct = BigDecimal.valueOf(newBalance)
                .divide(BigDecimal.valueOf(contract.getTotalSupply()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        holder.setBalance(newBalance);
        holder.setOwnershipPercentage(ownershipPct);
        holder.setBalanceUsd(contract.getTokenPriceUsd().multiply(BigDecimal.valueOf(newBalance)));
        if (newBalance == 0) holder.setStatus(TokenHolder.HolderStatus.EXITED);
        holderRepository.save(holder);
    }

    private void updateOrCreateRecipientHolder(UUID contractId, TokenContract contract, String toAddress,
                                               Long amount, BigDecimal tokenPrice) {
        var existing = holderRepository.findByTokenContractIdAndWalletAddress(
                contractId, toAddress);

        if (existing.isPresent()) {
            TokenHolder h = existing.get();
            updateHolderBalance(contract, h, h.getBalance() + amount);
        } else {
            BigDecimal ownershipPct = BigDecimal.valueOf(amount)
                    .divide(BigDecimal.valueOf(contract.getTotalSupply()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            var compliance = complianceClient.checkWallet(toAddress);
            UUID investorId = compliance != null && compliance.investorId() != null
                    ? compliance.investorId()
                    : UUID.randomUUID();

            TokenHolder newHolder = TokenHolder.builder()
                    .tokenContract(contract)
                    .investorId(investorId)
                    .walletAddress(toAddress)
                    .balance(amount)
                    .balanceUsd(tokenPrice.multiply(BigDecimal.valueOf(amount)))
                    .ownershipPercentage(ownershipPct)
                    .kycVerified(compliance != null && compliance.whitelisted())
                    .whitelistedOnChain(true)
                    .status(TokenHolder.HolderStatus.ACTIVE)
                    .build();
            holderRepository.save(newHolder);
        }
    }

    private String encodeTransfer(String toAddress, Long amount) {
        // keccak256("transfer(address,uint256)") = 0xa9059cbb
        String paddedAddress = padLeft(toAddress.replace("0x", ""), 64);
        String paddedAmount = padLeft(Long.toHexString(amount), 64);
        return "0xa9059cbb" + paddedAddress + paddedAmount;
    }

    private static String padLeft(String value, int length) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) sb.insert(0, '0');
        return sb.toString();
    }

    private TokenTransferResponse toResponse(TokenTransfer t) {
        return new TokenTransferResponse(
                t.getId(), t.getTokenContract().getId(),
                t.getFromAddress(), t.getToAddress(),
                t.getAmount(), t.getPricePerTokenUsd(), t.getTotalValueUsd(),
                t.getTxHash(), t.getBlockNumber(), t.getConfirmedAt(),
                t.getType(), t.getStatus(), t.getFailureReason(),
                t.getCreatedAt()
        );
    }
}