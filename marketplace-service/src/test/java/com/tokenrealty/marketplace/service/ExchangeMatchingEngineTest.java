package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.entity.ExchangeOrder;
import com.tokenrealty.marketplace.kafka.port.ExchangeEventPublisher;
import com.tokenrealty.marketplace.repository.ExchangeFillRepository;
import com.tokenrealty.marketplace.repository.ExchangeOrderRepository;
import com.tokenrealty.web.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exchange matching engine unit tests")
class ExchangeMatchingEngineTest {

    @Mock ExchangeOrderRepository exchangeOrderRepository;
    @Mock ExchangeFillRepository exchangeFillRepository;
    @Mock ExchangeSettlementService exchangeSettlementService;
    @Mock ExchangeEventPublisher exchangeEventPublisher;

    @InjectMocks ExchangeMatchingEngine exchangeMatchingEngine;

    UUID contractId = UUID.randomUUID();
    UUID investorId = UUID.randomUUID();

    @Test
    @DisplayName("Rejects wash trade when bid and ask share investor")
    void rejectsWashTrade() {
        ExchangeOrder bid = openOrder(ExchangeOrder.OrderSide.BID, new BigDecimal("100"), 5);
        ExchangeOrder ask = openOrder(ExchangeOrder.OrderSide.ASK, new BigDecimal("99"), 5);
        ask.setInvestorId(investorId);

        when(exchangeOrderRepository.findMatchingAsks(contractId, bid.getLimitPriceUsd()))
                .thenReturn(List.of(ask));

        assertThatThrownBy(() -> exchangeMatchingEngine.matchIncoming(bid))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Self-trading");

        verify(exchangeFillRepository, never()).save(any());
    }

    private ExchangeOrder openOrder(ExchangeOrder.OrderSide side, BigDecimal price, long qty) {
        return ExchangeOrder.builder()
                .contractId(contractId)
                .flatId(UUID.randomUUID())
                .buildingId(UUID.randomUUID())
                .side(side)
                .limitPriceUsd(price)
                .originalQuantity(qty)
                .filledQuantity(0L)
                .status(ExchangeOrder.OrderStatus.OPEN)
                .investorId(investorId)
                .walletAddress("0xabc")
                .liquidityTier("TIER_1")
                .build();
    }
}
