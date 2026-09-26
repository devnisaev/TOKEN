package com.tokenrealty.marketplace.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.client.PropertyRegistryClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.PlaceOrderRequest;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.command.PaymentConfirmedCommand;
import com.tokenrealty.marketplace.kafka.command.TransferCompletedCommand;
import com.tokenrealty.marketplace.repository.ListingRepository;
import com.tokenrealty.marketplace.repository.MarketOrderRepository;
import com.tokenrealty.marketplace.repository.TradeRepository;
import com.tokenrealty.marketplace.service.OrderService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Buy flow Kafka integration test")
class BuyFlowKafkaIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired ListingRepository listingRepository;
    @Autowired MarketOrderRepository orderRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired KafkaEventConsumer kafkaEventConsumer;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ComplianceClient complianceClient;
    @MockitoBean PaymentClient paymentClient;
    @MockitoBean PropertyRegistryClient propertyRegistryClient;

    private UUID flatId;
    private UUID contractId;
    private UUID buyerId;
    private Listing listing;

    @BeforeEach
    void seedListing() {
        flatId = UUID.randomUUID();
        contractId = UUID.randomUUID();
        buyerId = UUID.randomUUID();

        listing = listingRepository.save(Listing.builder()
                .flatId(flatId)
                .contractId(contractId)
                .listingType(Listing.ListingType.PRIMARY)
                .status(Listing.ListingStatus.ACTIVE)
                .priceUsd(new BigDecimal("10.00"))
                .tokensAvailable(100L)
                .tokensTotal(100L)
                .minInvestmentTokens(5L)
                .title("Primary offering — Flat 101")
                .build());

        when(complianceClient.isWalletApproved(any())).thenReturn(true);
        when(paymentClient.initiateTokenPurchase(any(), any(), any(), any()))
                .thenAnswer(invocation -> new PaymentClient.InitiatePaymentResponse(
                        UUID.randomUUID(),
                        invocation.getArgument(0),
                        null));
    }

    @Test
    void kafkaEventsFromPaymentAndIssuance_settleTradeAndReleaseEscrow() throws Exception {
        var orderResponse = orderService.placeBuyOrder(PlaceOrderRequest.builder()
                .listingId(listing.getId())
                .buyerId(buyerId)
                .buyerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .tokenAmount(10L)
                .build());

        MarketOrder order = orderRepository.findById(orderResponse.id()).orElseThrow();
        Trade trade = tradeRepository.findByOrderId(order.getId()).orElseThrow();
        UUID paymentId = trade.getPaymentId();

        consumePaymentConfirmed(UUID.randomUUID(), paymentId, order.getId(), buyerId, "0xPaymentTx");
        assertThat(tradeRepository.findByOrderId(order.getId()).orElseThrow().getStatus())
                .isEqualTo(Trade.TradeStatus.PAID);

        UUID transferId = UUID.randomUUID();
        consumeTransferCompleted(
                UUID.randomUUID(), transferId, contractId, flatId, order.getId(), trade.getId(), paymentId);

        MarketOrder settledOrder = orderRepository.findById(order.getId()).orElseThrow();
        Trade settledTrade = tradeRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(settledOrder.getStatus()).isEqualTo(MarketOrder.OrderStatus.SETTLED);
        assertThat(settledTrade.getStatus()).isEqualTo(Trade.TradeStatus.SETTLED);
        assertThat(settledTrade.getTransferId()).isEqualTo(transferId);
        verify(paymentClient).releaseEscrow(paymentId);
    }

    @Test
    void duplicatePaymentConfirmedEvent_isIgnored() throws Exception {
        var orderResponse = orderService.placeBuyOrder(PlaceOrderRequest.builder()
                .listingId(listing.getId())
                .buyerId(buyerId)
                .buyerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .tokenAmount(5L)
                .build());

        Trade trade = tradeRepository.findByOrderId(orderResponse.id()).orElseThrow();
        UUID eventId = UUID.randomUUID();
        String message = paymentConfirmedMessage(
                eventId, trade.getPaymentId(), orderResponse.id(), buyerId, "0xPaymentTx");

        kafkaEventConsumer.consume(message, MarketplaceKafkaEventTypes.PAYMENT_CONFIRMED,
                "Payment confirmed processing failed",
                event -> orderService.onPaymentConfirmed(PaymentConfirmedCommand.from(event)));
        kafkaEventConsumer.consume(message, MarketplaceKafkaEventTypes.PAYMENT_CONFIRMED,
                "Payment confirmed processing failed",
                event -> orderService.onPaymentConfirmed(PaymentConfirmedCommand.from(event)));

        assertThat(tradeRepository.findByOrderId(orderResponse.id()).orElseThrow().getStatus())
                .isEqualTo(Trade.TradeStatus.PAID);
    }

    private void consumePaymentConfirmed(
            UUID eventId, UUID paymentId, UUID orderId, UUID payerId, String txHash) throws Exception {
        String message = paymentConfirmedMessage(eventId, paymentId, orderId, payerId, txHash);
        kafkaEventConsumer.consume(message, MarketplaceKafkaEventTypes.PAYMENT_CONFIRMED,
                "Payment confirmed processing failed",
                event -> orderService.onPaymentConfirmed(PaymentConfirmedCommand.from(event)));
    }

    private void consumeTransferCompleted(
            UUID eventId,
            UUID transferId,
            UUID contractId,
            UUID flatId,
            UUID orderId,
            UUID tradeId,
            UUID paymentId) throws Exception {
        String message = transferCompletedMessage(
                eventId, transferId, contractId, flatId, orderId, tradeId, paymentId, "0xTransferTx");
        kafkaEventConsumer.consume(message, MarketplaceKafkaEventTypes.TRANSFER_COMPLETED,
                "Transfer completed processing failed",
                event -> {
                    TransferCompletedCommand command = TransferCompletedCommand.from(event);
                    UUID settledPaymentId = orderService.settleFromTransfer(command);
                    if (settledPaymentId != null) {
                        paymentClient.releaseEscrow(settledPaymentId);
                    }
                });
    }

    private String paymentConfirmedMessage(
            UUID eventId, UUID paymentId, UUID orderId, UUID payerId, String txHash) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", paymentId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("payerId", payerId.toString());
        payload.put("txHash", txHash);
        payload.put("confirmedAt", Instant.now().toString());
        return objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                MarketplaceKafkaEventTypes.PAYMENT_CONFIRMED,
                orderId.toString(),
                payload));
    }

    private String transferCompletedMessage(
            UUID eventId,
            UUID transferId,
            UUID contractId,
            UUID flatId,
            UUID orderId,
            UUID tradeId,
            UUID paymentId,
            String txHash) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferId", transferId.toString());
        payload.put("contractId", contractId.toString());
        payload.put("flatId", flatId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("tradeId", tradeId.toString());
        payload.put("paymentId", paymentId.toString());
        payload.put("txHash", txHash);
        payload.put("completedAt", Instant.now().toString());
        return objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                MarketplaceKafkaEventTypes.TRANSFER_COMPLETED,
                orderId.toString(),
                payload));
    }
}
