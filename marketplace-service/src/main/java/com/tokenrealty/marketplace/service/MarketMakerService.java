package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketMakerService {

    private final ExchangeOrderService exchangeOrderService;

    @Transactional
    public BulkExchangeOrderResponse submitBulkQuotes(BulkExchangeOrderRequest request) {
        List<ExchangeOrderResponse> placed = new ArrayList<>();
        for (PlaceExchangeOrderRequest order : request.orders()) {
            placed.add(exchangeOrderService.placeOrder(order));
        }
        return new BulkExchangeOrderResponse(placed.size(), placed);
    }
}
