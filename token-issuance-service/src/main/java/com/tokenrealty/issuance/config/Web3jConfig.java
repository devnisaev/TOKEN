package com.tokenrealty.issuance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

@Configuration
@Slf4j
public class Web3jConfig {

    private final BlockchainProperties props;

    public Web3jConfig(BlockchainProperties props) {
        this.props = props;
    }

    @Bean
    public Web3j web3j() {
        log.info("Connecting to blockchain network={} rpc={}",
                props.getNetwork(), props.getRpcUrl());
        return Web3j.build(new HttpService(props.getRpcUrl()));
    }

    @Bean
    public Credentials credentials() {
        return Credentials.create(props.getOperatorPrivateKey());
    }

    @Bean
    public ContractGasProvider gasProvider() {
        // 50 Gwei gas price, configurable gas limit
        BigInteger gasPrice = BigInteger.valueOf(50_000_000_000L);
        BigInteger gasLimit = BigInteger.valueOf(props.getGasLimit());
        return new StaticGasProvider(gasPrice, gasLimit);
    }
}