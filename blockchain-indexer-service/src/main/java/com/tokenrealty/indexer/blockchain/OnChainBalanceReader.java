package com.tokenrealty.indexer.blockchain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnChainBalanceReader {

    private final Web3j web3j;

    public long balanceOf(String contractAddress, String walletAddress) {
        try {
            Function function = new Function(
                    "balanceOf",
                    List.of(new Address(walletAddress)),
                    List.of(new TypeReference<Uint256>() {
                    }));
            String encoded = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, contractAddress, encoded),
                    DefaultBlockParameterName.LATEST).send();
            if (response.hasError()) {
                log.warn("balanceOf call failed contract={} wallet={}: {}",
                        contractAddress, walletAddress, response.getError().getMessage());
                return 0L;
            }
            List<org.web3j.abi.datatypes.Type> decoded = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());
            if (decoded.isEmpty()) {
                return 0L;
            }
            return ((Uint256) decoded.getFirst()).getValue().longValue();
        } catch (Exception ex) {
            log.warn("balanceOf failed contract={} wallet={}: {}", contractAddress, walletAddress, ex.getMessage());
            return 0L;
        }
    }

    public BigInteger latestBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (Exception ex) {
            log.warn("Failed to read latest block: {}", ex.getMessage());
            return BigInteger.ZERO;
        }
    }
}
