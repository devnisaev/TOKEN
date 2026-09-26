package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.client.TokenIssuanceClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.marketplace.kafka.command.PaymentConfirmedCommand;
import com.tokenrealty.marketplace.kafka.command.TransferCompletedCommand;
import com.tokenrealty.marketplace.kafka.port.OrderMatchedPublisher;
import com.tokenrealty.marketplace.kafka.port.TradeSettledPublisher;
import com.tokenrealty.marketplace.mapper.MarketplaceMapper;
import com.tokenrealty.marketplace.repository.ListingRepository;
import com.tokenrealty.marketplace.repository.MarketOrderRepository;
import com.tokenrealty.marketplace.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final MarketOrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final TradeRepository tradeRepository;
    private final TokenIssuanceClient tokenIssuanceClient;
    private final PaymentClient paymentClient;
    private final MarketplaceMapper mapper;
    private final OrderMatchedPublisher orderMatchedPublisher;
    private final TradeSettledPublisher tradeSettledPublisher;

    public Page<OrderResponse> findAll(UUID buyerId, UUID listingId, Pageable pageable) {
        if (buyerId != null) {
            return orderRepository.findByBuyerId(buyerId, pageable).map(mapper::toOrderResponse);
        }
        if (listingId != null) {
            return orderRepository.findByListingId(listingId, pageable).map(mapper::toOrderResponse);
        }
        return orderRepository.findAll(pageable).map(mapper::toOrderResponse);
    }

    public OrderResponse findById(UUID id) {
        return mapper.toOrderResponse(getOrder(id));
    }

    @Transactional
    public OrderResponse placeBuyOrder(PlaceOrderRequest request) {
        Listing listing = getActiveListing(request.listingId());
        validateOrderRequest(request, listing);
        assertBuyerWhitelisted(request.buyerWallet());

        listing.setTokensAvailable(listing.getTokensAvailable() - request.tokenAmount());
        if (listing.getTokensAvailable() == 0) {
            listing.setStatus(Listing.ListingStatus.SOLD);
        }

        BigDecimal totalPrice = listing.getPriceUsd()
                .multiply(BigDecimal.valueOf(request.tokenAmount()))
                .setScale(2, RoundingMode.HALF_UP);

        MarketOrder order = MarketOrder.builder()
                .listingId(listing.getId())
                .flatId(listing.getFlatId())
                .contractId(listing.getContractId())
                .orderType(MarketOrder.OrderType.BUY)
                .status(MarketOrder.OrderStatus.MATCHED)
                .buyerId(request.buyerId())
                .sellerId(listing.getSellerInvestorId())
                .buyerWallet(request.buyerWallet())
                .tokenAmount(request.tokenAmount())
                .totalPriceUsd(totalPrice)
                .build();

        MarketOrder savedOrder = orderRepository.save(order);
        Trade trade = createPendingTrade(savedOrder);
        linkEscrowPayment(savedOrder, trade);
        orderMatchedPublisher.publishOrderMatched(new OrderMatchedPublisher.OrderMatchedEvent(
                savedOrder.getId(),
                trade.getId(),
                savedOrder.getListingId(),
                savedOrder.getFlatId(),
                savedOrder.getContractId(),
                savedOrder.getBuyerId(),
                savedOrder.getSellerId(),
                savedOrder.getTokenAmount(),
                savedOrder.getTotalPriceUsd(),
                trade.getPaymentId()));
        return mapper.toOrderResponse(savedOrder);
    }

    @Transactional
    public TradeResponse settleOrder(UUID orderId, SettleTradeRequest request) {
        MarketOrder order = getOrder(orderId);
        Trade trade = tradeRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found for order: " + orderId));

        UUID paymentId = request.paymentId() != null ? request.paymentId() : trade.getPaymentId();
        if (paymentId == null) {
            raiseValidation("paymentId is required to settle trade");
        }
        trade.setPaymentId(paymentId);
        trade.setTransferId(request.transferId());
        trade.setStatus(Trade.TradeStatus.SETTLED);
        order.setStatus(MarketOrder.OrderStatus.SETTLED);

        tradeSettledPublisher.publishTradeSettled(new TradeSettledPublisher.TradeSettledEvent(
                trade.getId(),
                trade.getOrderId(),
                trade.getListingId(),
                trade.getPaymentId(),
                trade.getTransferId()));
        return mapper.toTradeResponse(trade);
    }

    public TradeResponse findTradeByOrderId(UUID orderId) {
        Trade trade = tradeRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found for order: " + orderId));
        MarketOrder order = getOrder(orderId);
        return mapper.toTradeResponse(trade, order);
    }

    @Transactional
    public void onPaymentConfirmed(PaymentConfirmedCommand command) {
        Trade trade = tradeRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found for order: " + command.orderId()));
        if (trade.getStatus() == Trade.TradeStatus.SETTLED) {
            return;
        }
        if (command.paymentId() != null) {
            trade.setPaymentId(command.paymentId());
        }
        trade.setStatus(Trade.TradeStatus.PAID);
    }

    @Transactional
    public UUID settleFromTransfer(TransferCompletedCommand command) {
        Trade trade = tradeRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Trade not found for order: " + command.orderId()));
        if (trade.getStatus() == Trade.TradeStatus.SETTLED) {
            return trade.getPaymentId();
        }
        if (command.transferId() != null) {
            trade.setTransferId(command.transferId());
        }
        if (command.paymentId() != null) {
            trade.setPaymentId(command.paymentId());
        }
        trade.setStatus(Trade.TradeStatus.SETTLED);
        MarketOrder order = getOrder(command.orderId());
        order.setStatus(MarketOrder.OrderStatus.SETTLED);
        tradeSettledPublisher.publishTradeSettled(new TradeSettledPublisher.TradeSettledEvent(
                trade.getId(),
                trade.getOrderId(),
                trade.getListingId(),
                trade.getPaymentId(),
                trade.getTransferId()));
        return trade.getPaymentId();
    }

    private void linkEscrowPayment(MarketOrder order, Trade trade) {
        PaymentClient.InitiatePaymentResponse payment = paymentClient.initiateTokenPurchase(
                order.getId(),
                order.getBuyerId(),
                order.getBuyerWallet(),
                order.getTotalPriceUsd());
        if (payment == null || payment.id() == null) {
            raiseValidation("Payment service returned empty response");
        }
        trade.setPaymentId(payment.id());
        tradeRepository.save(trade);
    }

    private Trade createPendingTrade(MarketOrder order) {
        Trade trade = Trade.builder()
                .orderId(order.getId())
                .listingId(order.getListingId())
                .flatId(order.getFlatId())
                .contractId(order.getContractId())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .tokenAmount(order.getTokenAmount())
                .totalPriceUsd(order.getTotalPriceUsd())
                .status(Trade.TradeStatus.PENDING)
                .build();
        return tradeRepository.save(trade);
    }

    private Listing getActiveListing(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new ValidationException("Listing is not active");
        }
        return listing;
    }

    private void validateOrderRequest(PlaceOrderRequest request, Listing listing) {
        if (request.tokenAmount() < listing.getMinInvestmentTokens()) {
            throw new ValidationException("tokenAmount below minimum investment");
        }
        if (request.tokenAmount() > listing.getTokensAvailable()) {
            throw new ValidationException("Not enough tokens available");
        }
    }

    private void assertBuyerWhitelisted(String wallet) {
        TokenIssuanceClient.ComplianceCheckResponse check = tokenIssuanceClient.checkWallet(wallet);
        if (check == null || !check.whitelisted()) {
            throw new ValidationException("Buyer wallet is not KYC whitelisted");
        }
    }

    private MarketOrder getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
