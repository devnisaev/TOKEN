package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.TokenIssuanceClient;
import com.tokenrealty.marketplace.client.ValuationClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.ExchangeOrderResponse;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.PlaceExchangeOrderRequest;
import com.tokenrealty.marketplace.entity.ExchangeFill;
import com.tokenrealty.marketplace.entity.ExchangeOrder;
import com.tokenrealty.marketplace.exchange.ExchangeLiquidityRules;
import com.tokenrealty.marketplace.kafka.port.ExchangeEventPublisher;
import com.tokenrealty.marketplace.repository.ExchangeOrderRepository;
import com.tokenrealty.web.exception.ComplianceBlockedException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeOrderService {

    private final ExchangeOrderRepository exchangeOrderRepository;
    private final ExchangeMatchingEngine exchangeMatchingEngine;
    private final ComplianceClient complianceClient;
    private final TokenIssuanceClient tokenIssuanceClient;
    private final ValuationClient valuationClient;
    private final ExchangeEventPublisher exchangeEventPublisher;

    @Value("${tokenrealty.exchange.nav-band-enabled:true}")
    private boolean navBandEnabled;

    public Page<ExchangeOrderResponse> findOpenOrders(UUID investorId, Pageable pageable) {
        return exchangeOrderRepository.findByInvestorIdAndStatusIn(
                        investorId,
                        List.of(
                                ExchangeOrder.OrderStatus.OPEN,
                                ExchangeOrder.OrderStatus.PARTIALLY_FILLED),
                        pageable)
                .map(this::toResponse);
    }

    public ExchangeOrderResponse findById(UUID id) {
        return toResponse(getOrder(id));
    }

    @Transactional
    public ExchangeOrderResponse placeOrder(PlaceExchangeOrderRequest request) {
        String tier = request.liquidityTier() != null ? request.liquidityTier() : "TIER_1";
        ExchangeLiquidityRules.assertClobAllowed(tier);
        ExchangeLiquidityRules.validatePriceTick(request.limitPriceUsd(), tier);
        ExchangeLiquidityRules.validateLotSize(request.quantity(), tier);

        if (navBandEnabled) {
            ValuationClient.NavSnapshotView nav = valuationClient.getLatestNav(request.flatId());
            if (nav != null && nav.navPerTokenUsd() != null) {
                ExchangeLiquidityRules.validateNavBand(request.limitPriceUsd(), nav.navPerTokenUsd(), tier);
            }
        }

        ComplianceClient.ComplianceCheckResponse compliance =
                assertWalletApproved(request.walletAddress(), "Investor");
        BigDecimal notional = request.limitPriceUsd()
                .multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        String country = compliance.countryCode() != null ? compliance.countryCode() : "US";
        complianceClient.checkInvestment(request.investorId(), country, notional);

        if (request.side() == ExchangeOrder.OrderSide.ASK) {
            long balance = tokenIssuanceClient.getHolderBalance(request.contractId(), request.walletAddress());
            if (balance < request.quantity()) {
                raiseValidation("Insufficient token balance for ask order");
            }
        }

        ExchangeOrder order = exchangeOrderRepository.save(ExchangeOrder.builder()
                .contractId(request.contractId())
                .flatId(request.flatId())
                .buildingId(request.buildingId())
                .side(request.side())
                .limitPriceUsd(request.limitPriceUsd())
                .originalQuantity(request.quantity())
                .filledQuantity(0L)
                .status(ExchangeOrder.OrderStatus.OPEN)
                .investorId(request.investorId())
                .walletAddress(request.walletAddress())
                .liquidityTier(tier)
                .build());

        exchangeEventPublisher.publishOrderPlaced(new ExchangeEventPublisher.OrderPlacedEvent(
                order.getId(),
                order.getContractId(),
                order.getFlatId(),
                order.getSide().name(),
                order.getLimitPriceUsd(),
                order.getOriginalQuantity(),
                order.getInvestorId()));

        List<ExchangeFill> fills = exchangeMatchingEngine.matchIncoming(order);
        return toResponse(order, fills.size());
    }

    @Transactional
    public ExchangeOrderResponse cancelOrder(UUID orderId, UUID investorId) {
        ExchangeOrder order = exchangeOrderRepository.findByIdAndInvestorId(orderId, investorId)
                .orElseThrow(() -> new ResourceNotFoundException("ExchangeOrder", orderId));
        if (order.getStatus() != ExchangeOrder.OrderStatus.OPEN
                && order.getStatus() != ExchangeOrder.OrderStatus.PARTIALLY_FILLED) {
            raiseValidation("Only open or partially filled orders can be cancelled");
        }
        long remaining = order.remainingQuantity();
        order.setStatus(ExchangeOrder.OrderStatus.CANCELLED);
        exchangeOrderRepository.save(order);

        exchangeEventPublisher.publishOrderCancelled(new ExchangeEventPublisher.OrderCancelledEvent(
                order.getId(),
                order.getContractId(),
                order.getInvestorId(),
                remaining));
        return toResponse(order);
    }

    private ExchangeOrder getOrder(UUID id) {
        return exchangeOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExchangeOrder", id));
    }

    private ExchangeOrderResponse toResponse(ExchangeOrder order) {
        return toResponse(order, 0);
    }

    private ExchangeOrderResponse toResponse(ExchangeOrder order, int fillsOnPlacement) {
        return ExchangeOrderResponse.builder()
                .id(order.getId())
                .contractId(order.getContractId())
                .flatId(order.getFlatId())
                .buildingId(order.getBuildingId())
                .side(order.getSide())
                .limitPriceUsd(order.getLimitPriceUsd())
                .originalQuantity(order.getOriginalQuantity())
                .filledQuantity(order.getFilledQuantity())
                .remainingQuantity(order.remainingQuantity())
                .status(order.getStatus())
                .investorId(order.getInvestorId())
                .walletAddress(order.getWalletAddress())
                .liquidityTier(order.getLiquidityTier())
                .fillsOnPlacement(fillsOnPlacement)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private ComplianceClient.ComplianceCheckResponse assertWalletApproved(String wallet, String role) {
        ComplianceClient.ComplianceCheckResponse response = complianceClient.checkWallet(wallet);
        if (response == null || !response.whitelisted()) {
            throw new ComplianceBlockedException(role + " wallet is not KYC approved");
        }
        return response;
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
