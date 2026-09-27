package com.tokenrealty.marketplace.integration;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.client.TokenIssuanceClient;
import com.tokenrealty.marketplace.client.ValuationClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.BookDepthResponse;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.ExchangeOrderResponse;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.PlaceExchangeOrderRequest;
import com.tokenrealty.marketplace.entity.ExchangeFill;
import com.tokenrealty.marketplace.entity.ExchangeOrder;
import com.tokenrealty.marketplace.repository.ExchangeFillRepository;
import com.tokenrealty.marketplace.repository.ExchangeOrderRepository;
import com.tokenrealty.marketplace.service.ExchangeMarketDataService;
import com.tokenrealty.marketplace.service.ExchangeOrderService;
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
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Exchange CLOB matching — Phase 13 integration")
class ExchangeMatchingIntegrationTest {

    @Autowired ExchangeOrderService exchangeOrderService;
    @Autowired ExchangeMarketDataService exchangeMarketDataService;
    @Autowired ExchangeOrderRepository exchangeOrderRepository;
    @Autowired ExchangeFillRepository exchangeFillRepository;

    @MockitoBean ComplianceClient complianceClient;
    @MockitoBean PaymentClient paymentClient;
    @MockitoBean ValuationClient valuationClient;
    @MockitoBean TokenIssuanceClient tokenIssuanceClient;

    UUID contractId;
    UUID flatId;
    UUID buildingId;
    UUID sellerId;
    UUID buyerId;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        flatId = UUID.randomUUID();
        buildingId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        buyerId = UUID.randomUUID();

        when(complianceClient.checkWallet(any())).thenAnswer(inv -> {
            String wallet = inv.getArgument(0);
            UUID investor = wallet.contains("seller") ? sellerId : buyerId;
            return new ComplianceClient.ComplianceCheckResponse(wallet, true, "APPROVED", investor, "US", null);
        });
        when(paymentClient.initiateTokenPurchase(any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new PaymentClient.InitiatePaymentResponse(UUID.randomUUID(), inv.getArgument(0), null));
        when(tokenIssuanceClient.getHolderBalance(any(), any())).thenReturn(100L);
        org.mockito.Mockito.doNothing().when(complianceClient).checkInvestment(any(), any(), any());
        when(valuationClient.getLatestNav(flatId)).thenReturn(new ValuationClient.NavSnapshotView(
                UUID.randomUUID(), flatId, buildingId, new BigDecimal("10000"), 100L,
                new BigDecimal("100.00"), java.time.Instant.now()));
    }

    @Test
    @DisplayName("Ask then crossing bid partially fills both sides")
    void partialFillOnCrossingBid() {
        exchangeOrderService.placeOrder(baseRequest(ExchangeOrder.OrderSide.ASK, "seller-wallet",
                sellerId, new BigDecimal("100.00"), 10L));

        ExchangeOrderResponse bid = exchangeOrderService.placeOrder(baseRequest(
                ExchangeOrder.OrderSide.BID, "buyer-wallet", buyerId, new BigDecimal("100.00"), 4L));

        assertThat(bid.fillsOnPlacement()).isEqualTo(1);
        assertThat(bid.filledQuantity()).isEqualTo(4L);
        assertThat(bid.remainingQuantity()).isEqualTo(0L);
        assertThat(bid.status()).isEqualTo(ExchangeOrder.OrderStatus.FILLED);

        ExchangeOrder ask = exchangeOrderRepository.findAll().stream()
                .filter(o -> o.getSide() == ExchangeOrder.OrderSide.ASK)
                .findFirst()
                .orElseThrow();
        assertThat(ask.getFilledQuantity()).isEqualTo(4L);
        assertThat(ask.getStatus()).isEqualTo(ExchangeOrder.OrderStatus.PARTIALLY_FILLED);

        assertThat(exchangeFillRepository.count()).isEqualTo(1);
        ExchangeFill fill = exchangeFillRepository.findAll().getFirst();
        assertThat(fill.getTokenAmount()).isEqualTo(4L);
        assertThat(fill.getPricePerTokenUsd()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Book depth aggregates open bid and ask levels")
    void bookDepthAggregation() {
        exchangeOrderService.placeOrder(baseRequest(ExchangeOrder.OrderSide.BID, "buyer-wallet",
                buyerId, new BigDecimal("95.00"), 5L));
        exchangeOrderService.placeOrder(baseRequest(ExchangeOrder.OrderSide.ASK, "seller-wallet",
                sellerId, new BigDecimal("105.00"), 8L));

        BookDepthResponse book = exchangeMarketDataService.getBook(contractId);
        assertThat(book.bids()).hasSize(1);
        assertThat(book.asks()).hasSize(1);
        assertThat(book.bids().getFirst().totalQuantity()).isEqualTo(5L);
        assertThat(book.asks().getFirst().totalQuantity()).isEqualTo(8L);
    }

    private PlaceExchangeOrderRequest baseRequest(
            ExchangeOrder.OrderSide side,
            String wallet,
            UUID investorId,
            BigDecimal price,
            long qty) {
        return PlaceExchangeOrderRequest.builder()
                .contractId(contractId)
                .flatId(flatId)
                .buildingId(buildingId)
                .side(side)
                .limitPriceUsd(price)
                .quantity(qty)
                .investorId(investorId)
                .walletAddress(wallet)
                .liquidityTier("TIER_1")
                .build();
    }
}
