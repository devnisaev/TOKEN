package com.tokenrealty.issuance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.issuance.dto.IssuanceDtos.DistributeDividendRequest;
import com.tokenrealty.issuance.entity.DividendPayment;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.PayoutCompletedCommand;
import com.tokenrealty.issuance.kafka.port.DividendDistributedPublisher;
import com.tokenrealty.issuance.repository.DividendPaymentRepository;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import com.tokenrealty.issuance.service.DividendService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Rent to dividend integration test")
class RentDividendIntegrationTest {

    @Autowired DividendService dividendService;
    @Autowired TokenContractRepository contractRepository;
    @Autowired TokenHolderRepository holderRepository;
    @Autowired DividendPaymentRepository dividendPaymentRepository;
    @Autowired KafkaEventConsumer kafkaEventConsumer;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean DividendDistributedPublisher dividendDistributedPublisher;

    private UUID contractId;
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
        contractId = contract.getId();

        holderRepository.save(TokenHolder.builder()
                .tokenContract(contract)
                .investorId(UUID.randomUUID())
                .walletAddress("0xHolder1")
                .balance(700L)
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build());
        holderRepository.save(TokenHolder.builder()
                .tokenContract(contract)
                .investorId(UUID.randomUUID())
                .walletAddress("0xHolder2")
                .balance(300L)
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build());
    }

    @Test
    void distributeFromRentCollected_createsPendingPaymentsAndPublishesEvent() {
        LocalDate periodStart = LocalDate.of(2025, 8, 1);
        LocalDate periodEnd = LocalDate.of(2025, 8, 31);

        var summary = dividendService.distribute(contractId, new DistributeDividendRequest(
                periodStart, periodEnd, new BigDecimal("1500.00")));

        assertThat(summary.recipientCount()).isEqualTo(2);
        assertThat(summary.totalDistributedUsd()).isEqualByComparingTo("1500.00");

        List<DividendPayment> payments = dividendPaymentRepository
                .findByTokenContractId(contractId, Pageable.unpaged()).getContent();
        assertThat(payments).hasSize(2);
        assertThat(payments).allMatch(p -> p.getStatus() == DividendPayment.PaymentStatus.PENDING);
        assertThat(payments.stream().map(DividendPayment::getAmountUsd).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("1500.00");

        verify(dividendDistributedPublisher).publishDividendDistributed(any());
    }

    @Test
    void payoutCompletedEvent_marksDividendPaymentPaid() throws Exception {
        LocalDate periodStart = LocalDate.of(2025, 8, 1);
        LocalDate periodEnd = LocalDate.of(2025, 8, 31);
        dividendService.distribute(contractId, new DistributeDividendRequest(
                periodStart, periodEnd, new BigDecimal("500.00")));

        DividendPayment payment = dividendPaymentRepository
                .findByTokenContractId(contractId, Pageable.unpaged()).getContent().getFirst();
        UUID eventId = UUID.randomUUID();
        String message = payoutCompletedMessage(
                eventId, payment.getId(), payment.getInvestorId(), "0xDividendPayoutTx");

        kafkaEventConsumer.consume(message, IssuanceKafkaEventTypes.PAYOUT_COMPLETED,
                "Payout completed processing failed",
                event -> {
                    PayoutCompletedCommand command = PayoutCompletedCommand.from(event);
                    dividendService.markPaid(
                            command.dividendPaymentId(), command.txHash(), command.completedAt());
                });

        DividendPayment updated = dividendPaymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DividendPayment.PaymentStatus.PAID);
        assertThat(updated.getTxHash()).isEqualTo("0xDividendPayoutTx");
        assertThat(updated.getPaidAt()).isNotNull();
    }

    private String payoutCompletedMessage(
            UUID eventId, UUID dividendPaymentId, UUID investorId, String txHash) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("payoutId", UUID.randomUUID().toString());
        payload.put("dividendPaymentId", dividendPaymentId.toString());
        payload.put("recipientInvestorId", investorId.toString());
        payload.put("recipientWallet", "0xHolder1");
        payload.put("purpose", "DIVIDEND");
        payload.put("txHash", txHash);
        payload.put("completedAt", Instant.now().toString());
        return objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                IssuanceKafkaEventTypes.PAYOUT_COMPLETED,
                dividendPaymentId.toString(),
                payload));
    }
}
