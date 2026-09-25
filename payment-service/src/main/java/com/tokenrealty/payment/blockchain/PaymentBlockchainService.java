package com.tokenrealty.payment.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.Optional;

/**
 * On-chain helpers for payment confirmation. MVP verifies tx existence; full USDC transfer parsing is Phase 2+.
 */
@Service
@Slf4j
public class PaymentBlockchainService {

    private final Web3j web3j;
    private final boolean enabled;

    public PaymentBlockchainService(@Value("${blockchain.rpc-url:}") String rpcUrl,
                                    @Value("${tokenrealty.payment.blockchain.enabled:false}") boolean enabled) {
        this.enabled = enabled && rpcUrl != null && !rpcUrl.isBlank();
        this.web3j = this.enabled ? Web3j.build(new HttpService(rpcUrl)) : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<TransactionReceipt> findReceipt(String txHash) {
        if (!enabled || txHash == null || txHash.isBlank()) {
            return Optional.empty();
        }
        try {
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash)
                    .send()
                    .getTransactionReceipt()
                    .orElse(null);
            if (receipt == null) {
                log.debug("No receipt yet for tx {}", txHash);
                return Optional.empty();
            }
            if (!receipt.isStatusOK()) {
                log.warn("Transaction {} failed on-chain", txHash);
                return Optional.empty();
            }
            return Optional.of(receipt);
        } catch (Exception ex) {
            log.warn("Blockchain lookup failed for {}: {}", txHash, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<BigInteger> blockNumber(String txHash) {
        return findReceipt(txHash).map(TransactionReceipt::getBlockNumber);
    }
}
