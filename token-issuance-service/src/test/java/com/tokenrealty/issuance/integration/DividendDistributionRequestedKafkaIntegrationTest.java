package com.tokenrealty.issuance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.issuance.entity.DividendPayment;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.DividendDistributionRequestedCommand;
import com.tokenrealty.issuance.kafka.port.DividendDistributedPublisher;
import com.tokenrealty.issuance.repository.DividendPaymentRepository;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import com.tokenrealty.issuance.service.RentDividendService;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class DividendDistributionRequestedKafkaIntegrationTest {

    @Autowired RentDividendService rentDividendService;
    @Autowired TokenContractRepository contractRepository;
    @Autowired TokenHolderRepository holderRepository;
    @Autowired DividendPaymentRepository dividendPaymentRepository;
    @Autowired KafkaEventConsumer kafkaEventConsumer;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean DividendDistributedPublisher dividendDistributedPublisher;

    private UUID flatId;

    @BeforeEach
    void seedContractAndHolders() {
        flatId = UUID.randomUUID();
        TokenContract contract = contractRepository.save(TokenContract.builder()
                .flatId(flatId)
                .buildingId(UUID.randomUUID())
                .spvWalletAddress("0xSPV")
                .totalSupply(1000L)
                .tokenPriceUsd(new BigDecimal("45.00"))
                .tokenName("Dividend Request Token")
                .tokenSymbol("DRT-101")
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
    @DisplayName("dividend.distribution-requested triggers pro-rata distribution")
    void dividendDistributionRequested_distributes() throws Exception {
        ingest(UUID.randomUUID(), flatId, "2025-09", "1200.00", UUID.randomUUID());

        List<DividendPayment> payments = dividendPaymentRepository
                .findByTokenContractId(
                        contractRepository.findByFlatId(flatId).orElseThrow().getId(),
                        Pageable.unpaged())
                .getContent();
        assertThat(payments).hasSize(2);
        assertThat(payments.stream().map(DividendPayment::getAmountUsd).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("1200.00");
        verify(dividendDistributedPublisher).publishDividendDistributed(any());
    }

    private void ingest(UUID corporateActionId, UUID flatId, String period, String grossAmountUsd, UUID eventId)
            throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("corporateActionId", corporateActionId.toString());
        payload.put("flatId", flatId.toString());
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("period", period);
        payload.put("grossAmountUsd", grossAmountUsd);

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                IssuanceKafkaEventTypes.DIVIDEND_DISTRIBUTION_REQUESTED,
                flatId.toString(),
                payload));
        kafkaEventConsumer.consume(message, IssuanceKafkaEventTypes.DIVIDEND_DISTRIBUTION_REQUESTED,
                "Dividend distribution requested processing failed",
                event -> rentDividendService.distributeForFlat(DividendDistributionRequestedCommand.from(event)));
    }
}
