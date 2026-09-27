package com.tokenrealty.marketplace.controller;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.service.ExchangeMarketDataService;
import com.tokenrealty.marketplace.service.ExchangeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/exchange")
@RequiredArgsConstructor
@Tag(name = "Exchange", description = "CLOB order book and market data (Phase 13)")
public class ExchangeController {

    private final ExchangeOrderService exchangeOrderService;
    private final ExchangeMarketDataService exchangeMarketDataService;

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Place a limit bid or ask on the order book")
    public ExchangeOrderResponse placeOrder(@Valid @RequestBody PlaceExchangeOrderRequest request) {
        return exchangeOrderService.placeOrder(request);
    }

    @DeleteMapping("/orders/{id}")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Cancel an open exchange order")
    public ExchangeOrderResponse cancelOrder(
            @PathVariable UUID id,
            @RequestParam UUID investorId) {
        return exchangeOrderService.cancelOrder(id, investorId);
    }

    @GetMapping("/orders")
    @Operation(summary = "List open exchange orders for an investor")
    public Page<ExchangeOrderResponse> listOpenOrders(
            @RequestParam UUID investorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return exchangeOrderService.findOpenOrders(investorId, pageable);
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get exchange order by id")
    public ExchangeOrderResponse getOrder(@PathVariable UUID id) {
        return exchangeOrderService.findById(id);
    }

    @GetMapping("/book/{contractId}")
    @Operation(summary = "Order book depth for a token contract")
    public BookDepthResponse getBook(@PathVariable UUID contractId) {
        return exchangeMarketDataService.getBook(contractId);
    }

    @GetMapping("/trades/{contractId}")
    @Operation(summary = "Recent exchange trades for a token contract")
    public Page<ExchangeTradeResponse> getTrades(
            @PathVariable UUID contractId,
            @PageableDefault(size = 20) Pageable pageable) {
        return exchangeMarketDataService.getRecentTrades(contractId, pageable);
    }

    @GetMapping("/ticker/{contractId}")
    @Operation(summary = "Ticker: last price, 24h volume, NAV delta")
    public ExchangeTickerResponse getTicker(
            @PathVariable UUID contractId,
            @RequestParam UUID flatId) {
        return exchangeMarketDataService.getTicker(contractId, flatId);
    }
}
