package com.tokenrealty.issuance.blockchain;

import com.tokenrealty.issuance.config.BlockchainProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.List;

/**
 * BlockchainConnector — low-level Web3j operations.
 *
 * All contract ABIs are called via dynamic ABI encoding since we don't
 * generate Java wrappers here (that requires a separate codegen step).
 * The service layer calls these methods and maps the results.
 *
 * For full web3j wrapper generation, run:
 *   web3j generate solidity -a artifacts/PropertyToken.json -o ../src/main/java -p com.tokenrealty.issuance.blockchain.wrapper
 */
@Component
@Slf4j
public class BlockchainConnector {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final BlockchainProperties props;
    private final TransactionManager txManager;

    public BlockchainConnector(Web3j web3j, Credentials credentials,
                               ContractGasProvider gasProvider,
                               BlockchainProperties props) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.gasProvider = gasProvider;
        this.props = props;
        this.txManager = new RawTransactionManager(web3j, credentials, props.getChainId());
    }

    // ─── Network info ────────────────────────────────────────────────────────

    public String getConnectedNetwork() {
        try {
            EthChainId chainId = web3j.ethChainId().send();
            return "chainId=" + chainId.getChainId().longValue()
                    + " network=" + props.getNetwork();
        } catch (Exception e) {
            log.error("Failed to get network info", e);
            return "unknown";
        }
    }

    public BigInteger getBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (Exception e) {
            log.error("Failed to get block number", e);
            return BigInteger.ZERO;
        }
    }

    public BigInteger getBalance(String address) {
        try {
            return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .send().getBalance();
        } catch (Exception e) {
            log.error("Failed to get balance for {}", address, e);
            return BigInteger.ZERO;
        }
    }

    // ─── Transaction receipt ─────────────────────────────────────────────────

    public TransactionReceipt waitForReceipt(String txHash) throws Exception {
        int attempts = 0;
        int maxAttempts = 40;
        int pollIntervalMs = 3000;

        while (attempts < maxAttempts) {
            EthGetTransactionReceipt receipt = web3j
                    .ethGetTransactionReceipt(txHash).send();

            if (receipt.getTransactionReceipt().isPresent()) {
                TransactionReceipt r = receipt.getTransactionReceipt().get();
                log.info("Tx {} confirmed in block {}", txHash, r.getBlockNumber());
                return r;
            }

            attempts++;
            log.debug("Waiting for tx {} (attempt {}/{})", txHash, attempts, maxAttempts);
            Thread.sleep(pollIntervalMs);
        }
        throw new RuntimeException("Transaction " + txHash + " not confirmed after "
                + (maxAttempts * pollIntervalMs / 1000) + "s");
    }

    public boolean isTransactionSuccessful(TransactionReceipt receipt) {
        // EIP-658: status "0x1" = success, "0x0" = failure
        return "0x1".equals(receipt.getStatus());
    }

    // ─── Raw function calls (ABI-encoded) ───────────────────────────────────

    /**
     * Call a read-only (view/pure) contract function.
     */
    public String callContractFunction(String contractAddress, String encodedData) {
        try {
            org.web3j.protocol.core.methods.request.Transaction tx =
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            credentials.getAddress(), contractAddress, encodedData);

            EthCall result = web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();
            if (result.hasError()) {
                throw new RuntimeException("Contract call error: " + result.getError().getMessage());
            }
            return result.getValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call contract function: " + e.getMessage(), e);
        }
    }

    /**
     * Send a state-changing transaction to a contract.
     * Returns the tx hash immediately (does not wait for confirmation).
     */
    public String sendContractTransaction(String contractAddress,
                                          String encodedData,
                                          BigInteger valueWei) throws Exception {
        BigInteger nonce = web3j
                .ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING)
                .send().getTransactionCount();

        BigInteger gasPrice = gasProvider.getGasPrice(null);
        BigInteger gasLimit = gasProvider.getGasLimit(null);

        EthSendTransaction response = txManager.sendTransaction(
                gasPrice, gasLimit, contractAddress, encodedData, valueWei);

        if (response.hasError()) {
            throw new RuntimeException("Transaction failed: " + response.getError().getMessage());
        }

        String txHash = response.getTransactionHash();
        log.info("Transaction submitted: {}", txHash);
        return txHash;
    }

    // ─── ABI function encoders ───────────────────────────────────────────────

    /**
     * Encodes: enableTransfers()
     */
    public static String encodeEnableTransfers() {
        return "0xaf35c6c7";
    }

    /**
     * Encodes: suspendTransfers()
     */
    public static String encodeSuspendTransfers() {
        return "0xf7f33ff4";
    }

    /**
     * Encodes: balanceOf(address)
     */
    public static String encodeBalanceOf(String address) {
        // keccak256("balanceOf(address)") = 0x70a08231
        String paddedAddress = padLeft(address.replace("0x", ""), 64);
        return "0x70a08231" + paddedAddress;
    }

    /**
     * Encodes: transfersEnabled()
     */
    public static String encodeTransfersEnabled() {
        return "0xbef97c87";
    }

    /**
     * Encodes: operatorTransfer(address from, address to, uint256 amount)
     */
    public static String encodeOperatorTransfer(String fromAddress, String toAddress, Long amount) {
        String paddedFrom = padLeft(fromAddress.replace("0x", ""), 64);
        String paddedTo = padLeft(toAddress.replace("0x", ""), 64);
        String paddedAmount = padLeft(Long.toHexString(amount), 64);
        return "0x0d1af103" + paddedFrom + paddedTo + paddedAmount;
    }

    /**
     * Encodes: isWhitelisted(address) — for ComplianceRegistry
     */
    public static String encodeIsWhitelisted(String address) {
        // keccak256("isWhitelisted(address)") first 4 bytes = 0x3af32abf
        String paddedAddress = padLeft(address.replace("0x", ""), 64);
        return "0x3af32abf" + paddedAddress;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String padLeft(String value, int length) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) sb.insert(0, '0');
        return sb.toString();
    }

    public static BigInteger decodeUint256(String hexResult) {
        if (hexResult == null || hexResult.equals("0x")) return BigInteger.ZERO;
        return new BigInteger(hexResult.replace("0x", ""), 16);
    }

    public static boolean decodeBool(String hexResult) {
        if (hexResult == null || hexResult.equals("0x")) return false;
        return !hexResult.replace("0x", "").replaceAll("0", "").isEmpty();
    }

    public Web3j getWeb3j() { return web3j; }
    public Credentials getCredentials() { return credentials; }
    public TransactionManager getTxManager() { return txManager; }
    public ContractGasProvider getGasProvider() { return gasProvider; }
}