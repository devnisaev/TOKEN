package com.tokenrealty.issuance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.issuance.entity.DividendPayment;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.RentCollectedCommand;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Rent collected Kafka integration test")
class RentCollectedKafkaIntegrationTest {

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
                .tokenName("Demo Flat Token")
                .tokenSymbol("DFT-101")
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
    @DisplayName("rent.collected triggers pro-rata dividend distribution")
    void rentCollected_distributesDividends() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID leaseId = UUID.randomUUID();

        ingestRentCollected(leaseId, flatId, tenantId, "2025-09", "1200.00", UUID.randomUUID());

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

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void rentCollected_dedupesDuplicateEventId() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID leaseId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        ingestRentCollected(leaseId, flatId, tenantId, "2025-10", "800.00", eventId);
        ingestRentCollected(leaseId, flatId, tenantId, "2025-10", "800.00", eventId);

        long paymentCount = dividendPaymentRepository.findAll().stream()
                .filter(p -> p.getPeriodStart().equals(java.time.LocalDate.of(2025, 10, 1)))
                .count();
        assertThat(paymentCount).isEqualTo(2);
        verify(dividendDistributedPublisher).publishDividendDistributed(any());
    }

    private void ingestRentCollected(
            UUID leaseId,
            UUID flatId,
            UUID tenantId,
            String period,
            String amountUsd,
            UUID eventId) throws Exception {
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("value", amountUsd);
        amount.put("currency", "USDC");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("payoutId", UUID.randomUUID().toString());
        payload.put("leaseId", leaseId.toString());
        payload.put("flatId", flatId.toString());
        payload.put("tenantId", tenantId.toString());
        payload.put("period", period);
        payload.put("amount", amount);
        payload.put("txHash", "0xRentTx");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                IssuanceKafkaEventTypes.RENT_COLLECTED,
                leaseId.toString(),
                payload));
        kafkaEventConsumer.consume(message, IssuanceKafkaEventTypes.RENT_COLLECTED,
                "Rent collected processing failed",
                event -> rentDividendService.distributeFromRent(RentCollectedCommand.from(event)));
    }
}
