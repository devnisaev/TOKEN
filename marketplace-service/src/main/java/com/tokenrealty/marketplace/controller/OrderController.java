package com.tokenrealty.marketplace.controller;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.service.OrderService;
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
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Buy and sell orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List orders")
    public Page<OrderResponse> list(
            @RequestParam(required = false) UUID buyerId,
            @RequestParam(required = false) UUID listingId,
            @PageableDefault(size = 20) Pageable pageable) {
        return orderService.findAll(buyerId, listingId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by id")
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Place a buy order on a listing")
    public OrderResponse placeBuyOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeBuyOrder(request);
    }

    @PostMapping("/sell")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Place a secondary sell order (creates listing)")
    public OrderResponse placeSellOrder(@Valid @RequestBody PlaceSellOrderRequest request) {
        return orderService.placeSellOrder(request);
    }

    @GetMapping("/{id}/trade")
    @Operation(summary = "Get trade for an order")
    public TradeResponse getTrade(@PathVariable UUID id) {
        return orderService.findTradeByOrderId(id);
    }

    @PatchMapping("/{id}/settle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark order settled after payment and on-chain transfer")
    public TradeResponse settle(
            @PathVariable UUID id,
            @Valid @RequestBody SettleTradeRequest request) {
        return orderService.settleOrder(id, request);
    }
}
