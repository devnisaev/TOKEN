package com.tokenrealty.issuance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.issuance.client.MarketplaceClient;
import com.tokenrealty.issuance.dto.IssuanceDtos.TokenTransferResponse;
import com.tokenrealty.issuance.dto.IssuanceDtos.TransferRequest;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenTransfer;
import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.PaymentConfirmedCommand;
import com.tokenrealty.issuance.kafka.port.TransferCompletedPublisher;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.service.PaymentTransferService;
import com.tokenrealty.issuance.service.TransferService;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Payment confirmed Kafka integration test")
class PaymentConfirmedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired PaymentTransferService paymentTransferService;
    @Autowired TokenContractRepository contractRepository;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean MarketplaceClient marketplaceClient;
    @MockitoBean TransferService transferService;
    @MockitoBean TransferCompletedPublisher transferCompletedPublisher;

    private UUID flatId;
    private UUID contractId;
    private UUID orderId;
    private UUID paymentId;
    private String buyerWallet;

    @BeforeEach
    void seedContract() {
        flatId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        buyerWallet = "0x70997970C51812dc3A010C724d1AfE6fc599aa84";

        TokenContract contract = contractRepository.save(TokenContract.builder()
                .flatId(flatId)
                .buildingId(UUID.randomUUID())
                .spvWalletAddress("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .totalSupply(1000L)
                .tokenPriceUsd(new BigDecimal("45.00"))
                .tokenName("Demo Flat Token")
                .tokenSymbol("DFT-101")
                .status(TokenContract.ContractStatus.ACTIVE)
                .network("localhost")
                .chainId(31337L)
                .build());
        contractId = contract.getId();
    }

    @Test
    @DisplayName("payment.confirmed executes token transfer and publishes transfer.completed")
    void paymentConfirmed_executesTransfer() throws Exception {
        UUID tradeId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        when(marketplaceClient.getTradeByOrderId(orderId)).thenReturn(new MarketplaceClient.TradeSnapshot(
                tradeId,
                orderId,
                flatId,
                contractId,
                UUID.randomUUID(),
                buyerWallet,
                null,
                "PRIMARY",
                100L,
                new BigDecimal("4500.00"),
                "MATCHED",
                paymentId,
                null));
        when(transferService.transfer(eq(contractId), any(TransferRequest.class))).thenReturn(
                new TokenTransferResponse(
                        transferId,
                        contractId,
                        "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
                        buyerWallet,
                        100L,
                        new BigDecimal("45.00"),
                        new BigDecimal("4500.00"),
                        "0xTransferTx",
                        1L,
                        Instant.parse("2025-09-25T16:00:00Z"),
                        TokenTransfer.TransferType.TRANSFER,
                        TokenTransfer.TransferStatus.CONFIRMED,
                        null,
                        Instant.parse("2025-09-25T16:00:00Z")));

        ingestPaymentConfirmed(UUID.randomUUID());

        verify(transferService).transfer(eq(contractId), any(TransferRequest.class));
        verify(transferCompletedPublisher).publishTransferCompleted(any());
    }

    @Test
    @DisplayName("payment.confirmed skips transfer when trade already settled")
    void paymentConfirmed_skipsWhenTradeSettled() throws Exception {
        when(marketplaceClient.getTradeByOrderId(orderId)).thenReturn(new MarketplaceClient.TradeSnapshot(
                UUID.randomUUID(),
                orderId,
                flatId,
                contractId,
                UUID.randomUUID(),
                buyerWallet,
                null,
                "PRIMARY",
                100L,
                new BigDecimal("4500.00"),
                "SETTLED",
                paymentId,
                UUID.randomUUID()));

        ingestPaymentConfirmed(UUID.randomUUID());

        verify(transferService, never()).transfer(any(), any());
        verify(transferCompletedPublisher, never()).publishTransferCompleted(any());
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void paymentConfirmed_dedupesDuplicateEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(marketplaceClient.getTradeByOrderId(orderId)).thenReturn(new MarketplaceClient.TradeSnapshot(
                UUID.randomUUID(),
                orderId,
                flatId,
                contractId,
                UUID.randomUUID(),
                buyerWallet,
                null,
                "PRIMARY",
                100L,
                new BigDecimal("4500.00"),
                "MATCHED",
                paymentId,
                null));
        when(transferService.transfer(eq(contractId), any(TransferRequest.class))).thenReturn(
                new TokenTransferResponse(
                        UUID.randomUUID(),
                        contractId,
                        "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
                        buyerWallet,
                        100L,
                        new BigDecimal("45.00"),
                        new BigDecimal("4500.00"),
                        "0xTransferTx",
                        1L,
                        Instant.now(),
                        TokenTransfer.TransferType.TRANSFER,
                        TokenTransfer.TransferStatus.CONFIRMED,
                        null,
                        Instant.now()));

        ingestPaymentConfirmed(eventId);
        ingestPaymentConfirmed(eventId);

        verify(transferService).transfer(eq(contractId), any(TransferRequest.class));
        verify(transferCompletedPublisher).publishTransferCompleted(any());
    }

    private void ingestPaymentConfirmed(UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", paymentId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("payerId", UUID.randomUUID().toString());
        payload.put("txHash", "0xPaymentConfirmed");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                IssuanceKafkaEventTypes.PAYMENT_CONFIRMED,
                orderId.toString(),
                payload));
        eventConsumer.consume(message, IssuanceKafkaEventTypes.PAYMENT_CONFIRMED,
                "Payment confirmed transfer failed",
                event -> paymentTransferService.executeTransfer(PaymentConfirmedCommand.from(event)));
    }
}
