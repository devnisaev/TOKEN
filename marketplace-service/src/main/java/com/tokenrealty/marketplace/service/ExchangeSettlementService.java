package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.entity.ExchangeFill;
import com.tokenrealty.marketplace.entity.ExchangeOrder;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import com.tokenrealty.marketplace.kafka.port.OrderMatchedPublisher;
import com.tokenrealty.marketplace.repository.ExchangeFillRepository;
import com.tokenrealty.marketplace.repository.MarketOrderRepository;
import com.tokenrealty.marketplace.repository.TradeRepository;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ExchangeSettlementService {

    private final MarketOrderRepository marketOrderRepository;
    private final TradeRepository tradeRepository;
    private final ExchangeFillRepository exchangeFillRepository;
    private final PaymentClient paymentClient;
    private final OrderMatchedPublisher orderMatchedPublisher;

    @Transactional
    public void settleFill(ExchangeFill fill, ExchangeOrder bid, ExchangeOrder ask) {
        MarketOrder settlementOrder = MarketOrder.builder()
                .listingId(null)
                .flatId(fill.getFlatId())
                .contractId(fill.getContractId())
                .listingType(Listing.ListingType.SECONDARY)
                .orderType(MarketOrder.OrderType.BUY)
                .status(MarketOrder.OrderStatus.MATCHED)
                .buyerId(fill.getBuyerId())
                .sellerId(fill.getSellerId())
                .buyerWallet(fill.getBuyerWallet())
                .sellerWallet(fill.getSellerWallet())
                .tokenAmount(fill.getTokenAmount())
                .totalPriceUsd(fill.getTotalPriceUsd())
                .build();
        MarketOrder savedOrder = marketOrderRepository.save(settlementOrder);

        Trade trade = Trade.builder()
                .orderId(savedOrder.getId())
                .listingId(null)
                .flatId(fill.getFlatId())
                .contractId(fill.getContractId())
                .listingType(Listing.ListingType.SECONDARY)
                .buyerId(fill.getBuyerId())
                .sellerId(fill.getSellerId())
                .sellerWallet(fill.getSellerWallet())
                .tokenAmount(fill.getTokenAmount())
                .totalPriceUsd(fill.getTotalPriceUsd())
                .status(Trade.TradeStatus.PENDING)
                .build();
        Trade savedTrade = tradeRepository.save(trade);

        PaymentClient.InitiatePaymentResponse payment = paymentClient.initiateTokenPurchase(
                savedOrder.getId(),
                fill.getBuyerId(),
                fill.getBuyerWallet(),
                fill.getTotalPriceUsd(),
                fill.getSellerId());
        if (payment == null || payment.id() == null) {
            raiseValidation("Payment service returned empty response");
        }
        savedTrade.setPaymentId(payment.id());
        tradeRepository.save(savedTrade);

        fill.setSettlementOrderId(savedOrder.getId());
        fill.setSettlementTradeId(savedTrade.getId());
        exchangeFillRepository.save(fill);

        orderMatchedPublisher.publishOrderMatched(new OrderMatchedPublisher.OrderMatchedEvent(
                savedOrder.getId(),
                savedTrade.getId(),
                null,
                fill.getFlatId(),
                fill.getContractId(),
                fill.getBuyerId(),
                fill.getSellerId(),
                fill.getTokenAmount(),
                fill.getTotalPriceUsd(),
                payment.id()));
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
