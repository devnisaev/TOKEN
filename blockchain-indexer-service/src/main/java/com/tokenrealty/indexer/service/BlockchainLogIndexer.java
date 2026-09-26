package com.tokenrealty.indexer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tokenrealty.indexer.blockchain.EventTopics;
import com.tokenrealty.indexer.blockchain.OnChainBalanceReader;
import com.tokenrealty.indexer.client.IssuanceClient;
import com.tokenrealty.indexer.entity.IndexedEvent;
import com.tokenrealty.indexer.entity.IndexerCursor;
import com.tokenrealty.indexer.kafka.outbox.OutboxIndexedEventPublisher;
import com.tokenrealty.indexer.metrics.IndexerMetrics;
import com.tokenrealty.indexer.repository.IndexedEventRepository;
import com.tokenrealty.indexer.repository.IndexerCursorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "tokenrealty.indexer.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class BlockchainLogIndexer {

    private final Web3j web3j;
    private final IssuanceClient issuanceClient;
    private final IndexerCursorRepository cursorRepository;
    private final IndexedEventRepository eventRepository;
    private final OnChainBalanceReader balanceReader;
    private final ObjectMapper objectMapper;
    private final OutboxIndexedEventPublisher indexedEventPublisher;
    private final IndexerMetrics indexerMetrics;

    @Value("${tokenrealty.indexer.batch-blocks:500}")
    private int batchBlocks;

    @Value("${tokenrealty.indexer.blockchain.compliance-registry-address:}")
    private String complianceRegistryAddress;

    @Value("${tokenrealty.indexer.websocket.enabled:false}")
    private boolean websocketEnabled;

    @Scheduled(fixedDelayString = "${tokenrealty.indexer.poll-ms:15000}")
    @Transactional
    public void pollLogs() {
        if (websocketEnabled) {
            return;
        }
        BigInteger latest = balanceReader.latestBlockNumber();
        if (latest.equals(BigInteger.ZERO)) {
            return;
        }
        IndexerCursor cursor = cursorRepository.findById(IndexerCursor.DEFAULT_ID)
                .orElseGet(() -> IndexerCursor.builder()
                        .id(IndexerCursor.DEFAULT_ID)
                        .lastBlockNumber(latest.subtract(BigInteger.ONE))
                        .build());

        BigInteger fromBlock = cursor.getLastBlockNumber().add(BigInteger.ONE);
        if (fromBlock.compareTo(latest) > 0) {
            return;
        }
        BigInteger toBlock = fromBlock.add(BigInteger.valueOf(batchBlocks - 1L));
        if (toBlock.compareTo(latest) > 0) {
            toBlock = latest;
        }

        Set<String> tracked = new HashSet<>(trackedAddresses());
        int indexed = 0;
        for (String address : tracked) {
            indexed += pollAddress(address, fromBlock, toBlock, tracked);
        }

        cursor.setLastBlockNumber(toBlock);
        cursorRepository.save(cursor);
        indexerMetrics.setBlockLag(latest.subtract(toBlock).longValue());
        indexerMetrics.recordEventsIndexed(indexed);
        if (indexed > 0) {
            log.info("Indexed {} events blocks {}-{}", indexed, fromBlock, toBlock);
        }
    }

    private int pollAddress(String address, BigInteger fromBlock, BigInteger toBlock, Set<String> tracked) {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                address);
        filter.addOptionalTopics(
                EventTopics.TRANSFER,
                EventTopics.WHITELIST_ADDED,
                EventTopics.WHITELIST_REMOVED,
                EventTopics.DIVIDEND_DEPOSITED,
                EventTopics.DIVIDEND_CLAIMED);

        try {
            List<Log> logs = web3j.ethGetLogs(filter).send().getLogs().stream()
                    .map(l -> (Log) l.get())
                    .filter(l -> tracked.contains(l.getAddress().toLowerCase()))
                    .toList();
            logs.forEach(this::persistLog);
            return logs.size();
        } catch (Exception ex) {
            log.warn("Log poll failed address={} blocks {}-{}: {}", address, fromBlock, toBlock, ex.getMessage());
            return 0;
        }
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

    @Transactional
    public void indexLog(Log logEntry) {
        persistLog(logEntry);
    }

    private void persistLog(Log logEntry) {
        if (eventRepository.existsByTxHashAndLogIndex(logEntry.getTransactionHash(), logEntry.getLogIndex().intValue())) {
            return;
        }
        String eventType = resolveEventType(logEntry.getTopics().getFirst());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("topic0", logEntry.getTopics().getFirst());
        payload.put("data", logEntry.getData());
        if (logEntry.getTopics().size() > 1) {
            payload.put("topic1", logEntry.getTopics().get(1));
        }
        if (logEntry.getTopics().size() > 2) {
            payload.put("topic2", logEntry.getTopics().get(2));
        }

        IndexedEvent saved = eventRepository.save(IndexedEvent.builder()
                .eventType(eventType)
                .contractAddress(logEntry.getAddress())
                .txHash(logEntry.getTransactionHash())
                .logIndex(logEntry.getLogIndex().intValue())
                .blockNumber(logEntry.getBlockNumber())
                .payload(payload.toString())
                .build());
        indexedEventPublisher.publishIndexedEvent(
                saved.getId(),
                eventType,
                logEntry.getAddress(),
                logEntry.getTransactionHash(),
                logEntry.getLogIndex().intValue(),
                logEntry.getBlockNumber().longValue());
    }

    private static String resolveEventType(String topic0) {
        if (EventTopics.TRANSFER.equals(topic0)) {
            return "Transfer";
        }
        if (EventTopics.WHITELIST_ADDED.equals(topic0)) {
            return "WhitelistAdded";
        }
        if (EventTopics.WHITELIST_REMOVED.equals(topic0)) {
            return "WhitelistRemoved";
        }
        if (EventTopics.DIVIDEND_DEPOSITED.equals(topic0)) {
            return "DividendDeposited";
        }
        if (EventTopics.DIVIDEND_CLAIMED.equals(topic0)) {
            return "DividendClaimed";
        }
        return "Unknown";
    }
}
