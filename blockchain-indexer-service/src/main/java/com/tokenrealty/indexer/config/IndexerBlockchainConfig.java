package com.tokenrealty.indexer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
@Slf4j
public class IndexerBlockchainConfig {

    @Bean
    Web3j web3j(@Value("${tokenrealty.indexer.blockchain.rpc-url}") String rpcUrl) {
        log.info("Indexer connecting to RPC {}", rpcUrl);
        return Web3j.build(new HttpService(rpcUrl));
    }
}
