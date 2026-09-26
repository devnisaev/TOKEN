package com.tokenrealty.marketplace.integration;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.client.PropertyRegistryClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.PlaceOrderRequest;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Buy flow integration test")
class BuyFlowIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired ListingRepository listingRepository;
    @Autowired MarketOrderRepository orderRepository;
    @Autowired TradeRepository tradeRepository;

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
    void buyToSettle_closesPrimaryPurchaseLoop() {
        var orderResponse = orderService.placeBuyOrder(PlaceOrderRequest.builder()
                .listingId(listing.getId())
                .buyerId(buyerId)
                .buyerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .tokenAmount(10L)
                .build());

        MarketOrder order = orderRepository.findById(orderResponse.id()).orElseThrow();
        Trade trade = tradeRepository.findByOrderId(order.getId()).orElseThrow();
        Listing updatedListing = listingRepository.findById(listing.getId()).orElseThrow();

        assertThat(order.getStatus()).isEqualTo(MarketOrder.OrderStatus.MATCHED);
        assertThat(trade.getStatus()).isEqualTo(Trade.TradeStatus.PENDING);
        assertThat(trade.getPaymentId()).isNotNull();
        assertThat(updatedListing.getTokensAvailable()).isEqualTo(90L);

        UUID paymentId = trade.getPaymentId();
        orderService.onPaymentConfirmed(new PaymentConfirmedCommand(
                UUID.randomUUID(), paymentId, order.getId(), buyerId, "0xPaymentTx"));

        Trade paidTrade = tradeRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(paidTrade.getStatus()).isEqualTo(Trade.TradeStatus.PAID);

        UUID transferId = UUID.randomUUID();
        UUID releasedPaymentId = orderService.settleFromTransfer(new TransferCompletedCommand(
                UUID.randomUUID(),
                transferId,
                contractId,
                flatId,
                order.getId(),
                trade.getId(),
                paymentId,
                "0xTransferTx"));

        MarketOrder settledOrder = orderRepository.findById(order.getId()).orElseThrow();
        Trade settledTrade = tradeRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(releasedPaymentId).isEqualTo(paymentId);
        assertThat(settledOrder.getStatus()).isEqualTo(MarketOrder.OrderStatus.SETTLED);
        assertThat(settledTrade.getStatus()).isEqualTo(Trade.TradeStatus.SETTLED);
        assertThat(settledTrade.getTransferId()).isEqualTo(transferId);
    }

    @Test
    void primarySellOut_marksFlatFullySold() {
        listing.setTokensAvailable(10L);
        listingRepository.save(listing);

        orderService.placeBuyOrder(PlaceOrderRequest.builder()
                .listingId(listing.getId())
                .buyerId(buyerId)
                .buyerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .tokenAmount(10L)
                .build());

        Listing soldListing = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(soldListing.getTokensAvailable()).isZero();
        assertThat(soldListing.getStatus()).isEqualTo(Listing.ListingStatus.SOLD);
        verify(propertyRegistryClient).markFlatFullySold(eq(flatId));
    }
}
