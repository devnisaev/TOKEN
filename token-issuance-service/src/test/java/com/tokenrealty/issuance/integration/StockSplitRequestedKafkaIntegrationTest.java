package com.tokenrealty.issuance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.StockSplitRequestedCommand;
import com.tokenrealty.issuance.kafka.port.StockSplitCompletedPublisher;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import com.tokenrealty.issuance.service.StockSplitService;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class StockSplitRequestedKafkaIntegrationTest {

    @Autowired StockSplitService stockSplitService;
    @Autowired TokenContractRepository contractRepository;
    @Autowired TokenHolderRepository holderRepository;
    @Autowired KafkaEventConsumer kafkaEventConsumer;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean StockSplitCompletedPublisher stockSplitCompletedPublisher;

    private UUID flatId;
    private UUID corporateActionId;

    @BeforeEach
    void seedContract() {
        flatId = UUID.randomUUID();
        corporateActionId = UUID.randomUUID();
        TokenContract contract = contractRepository.save(TokenContract.builder()
                .flatId(flatId)
                .buildingId(UUID.randomUUID())
                .spvWalletAddress("0xSPV")
                .totalSupply(1000L)
                .tokenPriceUsd(new BigDecimal("100.00"))
                .tokenName("Split Test Token")
                .tokenSymbol("STT-101")
                .status(TokenContract.ContractStatus.ACTIVE)
                .network("localhost")
                .chainId(31337L)
                .build());

        holderRepository.save(TokenHolder.builder()
                .tokenContract(contract)
                .investorId(UUID.randomUUID())
                .walletAddress("0xHolder1")
                .balance(600L)
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build());
        holderRepository.save(TokenHolder.builder()
                .tokenContract(contract)
                .investorId(UUID.randomUUID())
                .walletAddress("0xHolder2")
                .balance(400L)
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("stock-split.requested applies 2-for-1 split and publishes completion")
    void stockSplitRequested_appliesSplit() throws Exception {
        ingestStockSplitRequested(corporateActionId, flatId, "2025-Q3", "2.0000", UUID.randomUUID());

        TokenContract contract = contractRepository.findByFlatId(flatId).orElseThrow();
        assertThat(contract.getTotalSupply()).isEqualTo(2000L);
        assertThat(contract.getTokenPriceUsd()).isEqualByComparingTo("50.00");

        assertThat(holderRepository.findByTokenContractId(contract.getId()))
                .extracting(TokenHolder::getBalance)
                .containsExactlyInAnyOrder(1200L, 800L);
        verify(stockSplitCompletedPublisher).publishStockSplitCompleted(any());
    }

    private void ingestStockSplitRequested(
            UUID corporateActionId,
            UUID flatId,
            String period,
            String splitRatio,
            UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("corporateActionId", corporateActionId.toString());
        payload.put("flatId", flatId.toString());
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("period", period);
        payload.put("splitRatio", splitRatio);

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                IssuanceKafkaEventTypes.STOCK_SPLIT_REQUESTED,
                flatId.toString(),
                payload));
        kafkaEventConsumer.consume(message, IssuanceKafkaEventTypes.STOCK_SPLIT_REQUESTED,
                "Stock split requested processing failed",
                event -> stockSplitService.applySplit(StockSplitRequestedCommand.from(event)));
    }
}
