package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.entity.ExchangeFill;
import com.tokenrealty.marketplace.entity.ExchangeOrder;
import com.tokenrealty.marketplace.kafka.port.ExchangeEventPublisher;
import com.tokenrealty.marketplace.repository.ExchangeFillRepository;
import com.tokenrealty.marketplace.repository.ExchangeOrderRepository;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeMatchingEngine {

    private final ExchangeOrderRepository exchangeOrderRepository;
    private final ExchangeFillRepository exchangeFillRepository;
    private final ExchangeSettlementService exchangeSettlementService;
    private final ExchangeEventPublisher exchangeEventPublisher;
    private final Clock clock;

    public List<ExchangeFill> matchIncoming(ExchangeOrder incoming) {
        List<ExchangeFill> fills = new ArrayList<>();
        while (incoming.remainingQuantity() > 0 && isActive(incoming)) {
            ExchangeOrder counterparty = findCounterparty(incoming);
            if (counterparty == null) {
                break;
            }
            assertNotWashTrade(incoming, counterparty);
            ExchangeFill fill = executeMatch(incoming, counterparty);
            fills.add(fill);
        }
        exchangeOrderRepository.save(incoming);
        return fills;
    }

    private ExchangeOrder findCounterparty(ExchangeOrder incoming) {
        if (incoming.getSide() == ExchangeOrder.OrderSide.BID) {
            return exchangeOrderRepository.findMatchingAsks(incoming.getContractId(), incoming.getLimitPriceUsd())
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        return exchangeOrderRepository.findMatchingBids(incoming.getContractId(), incoming.getLimitPriceUsd())
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ExchangeFill executeMatch(ExchangeOrder incoming, ExchangeOrder counterparty) {
        ExchangeOrder bid = incoming.getSide() == ExchangeOrder.OrderSide.BID ? incoming : counterparty;
        ExchangeOrder ask = incoming.getSide() == ExchangeOrder.OrderSide.ASK ? incoming : counterparty;

        long fillQty = Math.min(incoming.remainingQuantity(), counterparty.remainingQuantity());
        BigDecimal executionPrice = ask.getLimitPriceUsd();
        BigDecimal totalPrice = executionPrice
                .multiply(BigDecimal.valueOf(fillQty))
                .setScale(2, RoundingMode.HALF_UP);

        applyFill(incoming, fillQty);
        applyFill(counterparty, fillQty);
        exchangeOrderRepository.save(counterparty);

        ExchangeFill fill = exchangeFillRepository.save(ExchangeFill.builder()
                .contractId(incoming.getContractId())
                .flatId(incoming.getFlatId())
                .bidOrderId(bid.getId())
                .askOrderId(ask.getId())
                .buyerId(bid.getInvestorId())
                .sellerId(ask.getInvestorId())
                .buyerWallet(bid.getWalletAddress())
                .sellerWallet(ask.getWalletAddress())
                .pricePerTokenUsd(executionPrice)
                .tokenAmount(fillQty)
                .totalPriceUsd(totalPrice)
                .executedAt(clock.instant())
                .build());

        exchangeEventPublisher.publishTradeExecuted(new ExchangeEventPublisher.TradeExecutedEvent(
                fill.getId(),
                fill.getContractId(),
                fill.getFlatId(),
                fill.getBidOrderId(),
                fill.getAskOrderId(),
                fill.getBuyerId(),
                fill.getSellerId(),
                fill.getPricePerTokenUsd(),
                fill.getTokenAmount(),
                fill.getTotalPriceUsd()));

        exchangeSettlementService.settleFill(fill, bid, ask);
        return fill;
    }

    private static void applyFill(ExchangeOrder order, long fillQty) {
        order.setFilledQuantity(order.getFilledQuantity() + fillQty);
        if (order.remainingQuantity() == 0) {
            order.setStatus(ExchangeOrder.OrderStatus.FILLED);
        } else {
            order.setStatus(ExchangeOrder.OrderStatus.PARTIALLY_FILLED);
        }
    }

    private static boolean isActive(ExchangeOrder order) {
        return order.getStatus() == ExchangeOrder.OrderStatus.OPEN
                || order.getStatus() == ExchangeOrder.OrderStatus.PARTIALLY_FILLED;
    }

    private static void assertNotWashTrade(ExchangeOrder incoming, ExchangeOrder counterparty) {
        if (incoming.getInvestorId().equals(counterparty.getInvestorId())) {
            throw new ValidationException("Self-trading is not permitted on the exchange order book");
        }
    }
}
