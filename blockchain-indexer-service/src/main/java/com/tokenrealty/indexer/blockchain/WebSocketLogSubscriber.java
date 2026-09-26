package com.tokenrealty.indexer.blockchain;

import com.tokenrealty.indexer.client.IssuanceClient;
import com.tokenrealty.indexer.service.BlockchainLogIndexer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "tokenrealty.indexer.websocket.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class WebSocketLogSubscriber {

    private final @Qualifier("indexerWebSocketWeb3j") Web3j webSocketWeb3j;
    private final IssuanceClient issuanceClient;
    private final BlockchainLogIndexer logIndexer;

    @Value("${tokenrealty.indexer.blockchain.compliance-registry-address:}")
    private String complianceRegistryAddress;

    private final Map<String, io.reactivex.disposables.Disposable> subscriptionsByAddress =
            new ConcurrentHashMap<>();

    @PostConstruct
    void subscribe() {
        refreshSubscriptions();
    }

    @Scheduled(fixedRate = 60_000)
    void refreshSubscriptions() {
        Set<String> tracked = new HashSet<>(trackedAddresses());
        subscriptionsByAddress.keySet().stream()
                .filter(address -> !tracked.contains(address))
                .forEach(this::unsubscribeAddress);
        tracked.forEach(this::subscribeToAddress);
    }

    @PreDestroy
    void unsubscribe() {
        subscriptionsByAddress.keySet().forEach(this::unsubscribeAddress);
    }

    private void subscribeToAddress(String address) {
        String normalized = address.toLowerCase();
        if (subscriptionsByAddress.containsKey(normalized)) {
            return;
        }
        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                normalized);
        filter.addOptionalTopics(
                EventTopics.TRANSFER,
                EventTopics.WHITELIST_ADDED,
                EventTopics.WHITELIST_REMOVED,
                EventTopics.DIVIDEND_DEPOSITED,
                EventTopics.DIVIDEND_CLAIMED);
        var subscription = webSocketWeb3j.ethLogFlowable(filter).subscribe(
                this::handleLog,
                error -> {
                    log.warn("WebSocket log subscription error address={}: {}", normalized, error.getMessage());
                    unsubscribeAddress(normalized);
                    subscribeToAddress(normalized);
                });
        subscriptionsByAddress.put(normalized, subscription);
        log.info("WebSocket log subscription active for {}", normalized);
    }

    private void unsubscribeAddress(String address) {
        io.reactivex.disposables.Disposable subscription = subscriptionsByAddress.remove(address);
        if (subscription != null) {
            subscription.dispose();
            log.info("WebSocket log subscription removed for {}", address);
        }
    }

    private void handleLog(Log logEntry) {
        logIndexer.indexLog(logEntry);
    }

    private List<String> trackedAddresses() {
        List<String> addresses = new ArrayList<>();
        issuanceClient.listContracts().stream()
                .map(IssuanceClient.TokenContractView::contractAddress)
                .filter(a -> a != null && !a.isBlank())
                .map(String::toLowerCase)
                .forEach(addresses::add);
        if (complianceRegistryAddress != null && !complianceRegistryAddress.isBlank()) {
            addresses.add(complianceRegistryAddress.toLowerCase());
        }
        return addresses;
    }
}
