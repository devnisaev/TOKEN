package com.tokenrealty.indexer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import java.net.ConnectException;

@Configuration
@Slf4j
public class IndexerBlockchainConfig {

    @Bean
    Web3j web3j(@Value("${tokenrealty.indexer.blockchain.rpc-url}") String rpcUrl) {
        log.info("Indexer connecting to RPC {}", rpcUrl);
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean(name = "indexerWebSocketWeb3j")
    @ConditionalOnProperty(name = "tokenrealty.indexer.websocket.enabled", havingValue = "true")
    Web3j indexerWebSocketWeb3j(
            @Value("${tokenrealty.indexer.websocket.rpc-url:ws://localhost:8545}") String wsUrl) {
        log.info("Indexer WebSocket connecting to {}", wsUrl);
        WebSocketService ws = new WebSocketService(wsUrl, false);
        try {
            ws.connect();
        } catch (ConnectException ex) {
            throw new IllegalStateException("Failed to connect WebSocket RPC: " + wsUrl, ex);
        }
        return Web3j.build(ws);
    }
}
