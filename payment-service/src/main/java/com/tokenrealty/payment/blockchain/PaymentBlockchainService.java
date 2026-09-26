package com.tokenrealty.payment.blockchain;

import com.tokenrealty.payment.config.PaymentBlockchainProperties;
import com.tokenrealty.payment.entity.PaymentCurrency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * On-chain helpers for payment confirmation and ERC-20 payouts (USDC / MockUSDC).
 */
@Service
@Slf4j
public class PaymentBlockchainService {

    private static final int USDC_DECIMALS = 6;

    private final Web3j web3j;
    private final Credentials credentials;
    private final RawTransactionManager txManager;
    private final PaymentBlockchainProperties props;
    private final boolean enabled;

    public PaymentBlockchainService(PaymentBlockchainProperties props) {
        this.props = props;
        this.enabled = props.isEnabled()
                && props.getRpcUrl() != null && !props.getRpcUrl().isBlank()
                && props.getOperatorPrivateKey() != null && !props.getOperatorPrivateKey().isBlank()
                && props.getUsdcContractAddress() != null && !props.getUsdcContractAddress().isBlank();
        if (enabled) {
            this.web3j = Web3j.build(new HttpService(props.getRpcUrl()));
            this.credentials = Credentials.create(props.getOperatorPrivateKey());
            this.txManager = new RawTransactionManager(web3j, credentials, props.getChainId());
        } else {
            this.web3j = null;
            this.credentials = null;
            this.txManager = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<TransactionReceipt> findReceipt(String txHash) {
        if (!enabled || txHash == null || txHash.isBlank() || txHash.startsWith("0xSIMULATED")) {
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

    /**
     * Sends ERC-20 transfer from operator wallet. Amount is USD with 2 decimal places (e.g. 50.00 USDC).
     */
    public String sendTokenTransfer(String recipientWallet, BigDecimal amount, PaymentCurrency currency) {
        if (!enabled) {
            throw new IllegalStateException("Payment blockchain is not configured");
        }
        if (currency != PaymentCurrency.USDC) {
            throw new IllegalArgumentException("On-chain payout supports USDC only");
        }
        try {
            BigInteger tokenAmount = toTokenUnits(amount);
            String encoded = encodeErc20Transfer(recipientWallet, tokenAmount);
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            EthSendTransaction response = txManager.sendTransaction(
                    gasPrice,
                    BigInteger.valueOf(props.getGasLimit()),
                    props.getUsdcContractAddress(),
                    encoded,
                    BigInteger.ZERO);
            if (response.hasError()) {
                throw new IllegalStateException("Transfer failed: " + response.getError().getMessage());
            }
            String txHash = response.getTransactionHash();
            log.info("On-chain {} transfer to {} tx={}", currency, recipientWallet, txHash);
            return txHash;
        } catch (Exception ex) {
            throw new IllegalStateException("On-chain transfer failed", ex);
        }
    }

    public boolean hasSufficientMaticForGas() {
        if (!enabled) {
            return false;
        }
        try {
            BigInteger balance = web3j.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                    .send()
                    .getBalance();
            return balance.compareTo(Convert.toWei("0.001", Convert.Unit.ETHER).toBigInteger()) >= 0;
        } catch (Exception ex) {
            log.warn("Failed to read operator MATIC balance: {}", ex.getMessage());
            return false;
        }
    }

    static BigInteger toTokenUnits(BigDecimal amount) {
        return amount.movePointRight(USDC_DECIMALS).setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }

    static String encodeErc20Transfer(String toAddress, BigInteger amount) {
        String paddedAddress = padLeft(toAddress.replace("0x", ""), 64);
        String paddedAmount = padLeft(amount.toString(16), 64);
        return "0xa9059cbb" + paddedAddress + paddedAmount;
    }

    private static String padLeft(String value, int length) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
}
