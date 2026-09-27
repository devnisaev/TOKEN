package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.client.ValuationClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.BookDepthResponse;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.BookLevel;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.ExchangeTickerResponse;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.ExchangeTradeResponse;
import com.tokenrealty.marketplace.entity.ExchangeFill;
import com.tokenrealty.marketplace.entity.ExchangeOrder;
import com.tokenrealty.marketplace.repository.ExchangeFillRepository;
import com.tokenrealty.marketplace.repository.ExchangeOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeMarketDataService {

    private final ExchangeOrderRepository exchangeOrderRepository;
    private final ExchangeFillRepository exchangeFillRepository;
    private final ValuationClient valuationClient;
    private final Clock clock;

    public BookDepthResponse getBook(UUID contractId) {
        List<ExchangeOrder> openOrders = exchangeOrderRepository.findOpenBook(contractId);
        Map<BigDecimal, LevelAccumulator> bids = new LinkedHashMap<>();
        Map<BigDecimal, LevelAccumulator> asks = new LinkedHashMap<>();

        for (ExchangeOrder order : openOrders) {
            Map<BigDecimal, LevelAccumulator> side =
                    order.getSide() == ExchangeOrder.OrderSide.BID ? bids : asks;
            side.computeIfAbsent(order.getLimitPriceUsd(), LevelAccumulator::new)
                    .add(order.remainingQuantity());
        }

        return BookDepthResponse.builder()
                .contractId(contractId)
                .bids(toLevels(bids, true))
                .asks(toLevels(asks, false))
                .asOf(clock.instant())
                .build();
    }

    public Page<ExchangeTradeResponse> getRecentTrades(UUID contractId, Pageable pageable) {
        return exchangeFillRepository.findByContractIdOrderByExecutedAtDesc(contractId, pageable)
                .map(ExchangeTradeResponse::from);
    }

    public ExchangeTickerResponse getTicker(UUID contractId, UUID flatId) {
        Instant since = clock.instant().minus(24, ChronoUnit.HOURS);
        ExchangeFill lastFill = exchangeFillRepository.findTopByContractIdOrderByExecutedAtDesc(contractId)
                .orElse(null);
        BigDecimal lastPrice = lastFill != null ? lastFill.getPricePerTokenUsd() : null;
        long volume24h = exchangeFillRepository.sumVolumeSince(contractId, since);
        BigDecimal notional24h = exchangeFillRepository.sumNotionalSince(contractId, since);

        BigDecimal navPerToken = null;
        BigDecimal navDeltaPct = null;
        ValuationClient.NavSnapshotView nav = valuationClient.getLatestNav(flatId);
        if (nav != null) {
            navPerToken = nav.navPerTokenUsd();
            if (lastPrice != null && navPerToken != null && navPerToken.signum() > 0) {
                navDeltaPct = lastPrice.subtract(navPerToken)
                        .divide(navPerToken, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        return ExchangeTickerResponse.builder()
                .contractId(contractId)
                .lastPriceUsd(lastPrice)
                .navPerTokenUsd(navPerToken)
                .navDeltaPct(navDeltaPct)
                .volume24hTokens(volume24h)
                .notional24hUsd(notional24h)
                .asOf(clock.instant())
                .build();
    }

    private static List<BookLevel> toLevels(Map<BigDecimal, LevelAccumulator> levels, boolean descending) {
        Comparator<BookLevel> comparator = descending
                ? Comparator.comparing(BookLevel::priceUsd).reversed()
                : Comparator.comparing(BookLevel::priceUsd);
        List<BookLevel> result = new ArrayList<>();
        levels.values().stream()
                .map(LevelAccumulator::toLevel)
                .sorted(comparator)
                .forEach(result::add);
        return result;
    }

    private static final class LevelAccumulator {
        private final BigDecimal price;
        private long quantity;
        private int count;

        private LevelAccumulator(BigDecimal price) {
            this.price = price;
        }

        private void add(long amount) {
            quantity += amount;
            count++;
        }

        private BookLevel toLevel() {
            return BookLevel.builder()
                    .priceUsd(price)
                    .totalQuantity(quantity)
                    .orderCount(count)
                    .build();
        }
    }
}
