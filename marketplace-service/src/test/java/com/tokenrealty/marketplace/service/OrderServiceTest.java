package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.PlaceOrderRequest;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.OrderResponse;
import com.tokenrealty.marketplace.kafka.command.PaymentConfirmedCommand;
import com.tokenrealty.marketplace.kafka.command.TransferCompletedCommand;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.marketplace.kafka.port.OrderMatchedPublisher;
import com.tokenrealty.marketplace.kafka.port.TradeSettledPublisher;
import com.tokenrealty.marketplace.mapper.MarketplaceMapper;
import com.tokenrealty.marketplace.repository.ListingRepository;
import com.tokenrealty.marketplace.repository.MarketOrderRepository;
import com.tokenrealty.marketplace.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService unit tests")
class OrderServiceTest {

    @Mock MarketOrderRepository orderRepository;
    @Mock ListingRepository listingRepository;
    @Mock TradeRepository tradeRepository;
    @Mock ComplianceClient complianceClient;
    @Mock PaymentClient paymentClient;
    @Mock ListingService listingService;
    @Mock MarketplaceMapper mapper;
    @Mock OrderMatchedPublisher orderMatchedPublisher;
    @Mock TradeSettledPublisher tradeSettledPublisher;
    @InjectMocks OrderService orderService;

    private UUID listingId;
    private Listing listing;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        listing = Listing.builder()
                .flatId(UUID.randomUUID())
                .listingType(Listing.ListingType.PRIMARY)
                .status(Listing.ListingStatus.ACTIVE)
                .priceUsd(new BigDecimal("10.00"))
                .tokensAvailable(100L)
                .tokensTotal(100L)
                .minInvestmentTokens(5L)
                .build();
        listing.setId(listingId);
    }

    @Test
    @DisplayName("placeBuyOrder rejects non-whitelisted wallet")
    void rejectsNonWhitelistedWallet() {
        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .listingId(listingId)
                .buyerId(UUID.randomUUID())
                .buyerWallet("0xabc")
                .tokenAmount(10L)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(complianceClient.isWalletApproved("0xabc")).thenReturn(false);

        assertThatThrownBy(() -> orderService.placeBuyOrder(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("placeBuyOrder matches whitelisted buyer")
    void matchesWhitelistedBuyer() {
        UUID buyerId = UUID.randomUUID();
        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .listingId(listingId)
                .buyerId(buyerId)
                .buyerWallet("0xabc")
                .tokenAmount(10L)
                .build();

        MarketOrder savedOrder = MarketOrder.builder()
                .listingId(listingId)
                .flatId(listing.getFlatId())
                .listingType(Listing.ListingType.PRIMARY)
                .orderType(MarketOrder.OrderType.BUY)
                .status(MarketOrder.OrderStatus.MATCHED)
                .buyerId(buyerId)
                .buyerWallet("0xabc")
                .tokenAmount(10L)
                .totalPriceUsd(new BigDecimal("100.00"))
                .build();
        savedOrder.setId(UUID.randomUUID());

        Trade trade = Trade.builder()
                .orderId(savedOrder.getId())
                .listingId(listingId)
                .flatId(listing.getFlatId())
                .listingType(Listing.ListingType.PRIMARY)
                .buyerId(buyerId)
                .tokenAmount(10L)
                .totalPriceUsd(new BigDecimal("100.00"))
                .status(Trade.TradeStatus.PENDING)
                .build();
        trade.setId(UUID.randomUUID());

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(complianceClient.isWalletApproved("0xabc")).thenReturn(true);
        UUID paymentId = UUID.randomUUID();
        when(orderRepository.save(any(MarketOrder.class))).thenReturn(savedOrder);
        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);
        when(paymentClient.initiateTokenPurchase(
                savedOrder.getId(), buyerId, "0xabc", new BigDecimal("100.00")))
                .thenReturn(new PaymentClient.InitiatePaymentResponse(
                        paymentId, savedOrder.getId(), null));
        when(mapper.toOrderResponse(savedOrder)).thenReturn(OrderResponse.builder().id(savedOrder.getId()).build());

        orderService.placeBuyOrder(request);

        verify(paymentClient).initiateTokenPurchase(
                savedOrder.getId(), buyerId, "0xabc", new BigDecimal("100.00"));
        verify(tradeRepository, times(2)).save(any(Trade.class));
        verify(orderMatchedPublisher).publishOrderMatched(any());
        verify(orderRepository).save(any(MarketOrder.class));
    }

    @Test
    @DisplayName("onPaymentConfirmed marks trade PAID")
    void onPaymentConfirmedMarksPaid() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Trade trade = Trade.builder()
                .orderId(orderId)
                .status(Trade.TradeStatus.PENDING)
                .build();
        when(tradeRepository.findByOrderId(orderId)).thenReturn(Optional.of(trade));

        orderService.onPaymentConfirmed(new PaymentConfirmedCommand(
                UUID.randomUUID(), paymentId, orderId, UUID.randomUUID(), "0xabc"));

        assertThat(trade.getStatus()).isEqualTo(Trade.TradeStatus.PAID);
        assertThat(trade.getPaymentId()).isEqualTo(paymentId);
    }

    @Test
    @DisplayName("settleFromTransfer marks trade SETTLED and publishes event")
    void settleFromTransfer() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        MarketOrder order = MarketOrder.builder()
                .status(MarketOrder.OrderStatus.MATCHED)
                .build();
        order.setId(orderId);
        Trade trade = Trade.builder()
                .orderId(orderId)
                .listingId(UUID.randomUUID())
                .status(Trade.TradeStatus.PAID)
                .paymentId(paymentId)
                .build();
        trade.setId(UUID.randomUUID());

        when(tradeRepository.findByOrderId(orderId)).thenReturn(Optional.of(trade));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        UUID result = orderService.settleFromTransfer(new TransferCompletedCommand(
                UUID.randomUUID(), transferId, UUID.randomUUID(), UUID.randomUUID(),
                orderId, trade.getId(), paymentId, "0xtx"));

        assertThat(result).isEqualTo(paymentId);
        assertThat(trade.getStatus()).isEqualTo(Trade.TradeStatus.SETTLED);
        assertThat(trade.getTransferId()).isEqualTo(transferId);
        verify(tradeSettledPublisher).publishTradeSettled(any());
    }
}
