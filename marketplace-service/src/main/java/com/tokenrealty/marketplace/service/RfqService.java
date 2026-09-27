package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.*;
import com.tokenrealty.marketplace.kafka.port.OrderMatchedPublisher;
import com.tokenrealty.marketplace.repository.MarketOrderRepository;
import com.tokenrealty.marketplace.repository.RfqQuoteRepository;
import com.tokenrealty.marketplace.repository.RfqRequestRepository;
import com.tokenrealty.marketplace.repository.TradeRepository;
import com.tokenrealty.web.exception.ComplianceBlockedException;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RfqService {

    private static final BigDecimal MIN_OTC_NOTIONAL = new BigDecimal("500000.00");

    private final RfqRequestRepository rfqRequestRepository;
    private final RfqQuoteRepository rfqQuoteRepository;
    private final MarketOrderRepository marketOrderRepository;
    private final TradeRepository tradeRepository;
    private final ComplianceClient complianceClient;
    private final PaymentClient paymentClient;
    private final OrderMatchedPublisher orderMatchedPublisher;
    private final Clock clock;

    public Page<RfqRequestResponse> listOpen(Pageable pageable) {
        return rfqRequestRepository.findByStatusOrderByCreatedAtDesc(RfqRequest.RfqStatus.OPEN, pageable)
                .map(this::toRequestResponse);
    }

    public Page<RfqRequestResponse> listByRequester(UUID requesterId, Pageable pageable) {
        return rfqRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId, pageable)
                .map(this::toRequestResponse);
    }

    public RfqRequestResponse findById(UUID id) {
        return toRequestResponse(getRequest(id));
    }

    public List<RfqQuoteResponse> listQuotes(UUID rfqId) {
        getRequest(rfqId);
        return rfqQuoteRepository.findByRfqRequestIdOrderByCreatedAtDesc(rfqId).stream()
                .map(this::toQuoteResponse)
                .toList();
    }

    @Transactional
    public RfqRequestResponse createRequest(CreateRfqRequest request) {
        assertWalletApproved(request.walletAddress());
        BigDecimal notional = request.indicativePriceUsd().multiply(BigDecimal.valueOf(request.tokenAmount()));
        if (notional.compareTo(MIN_OTC_NOTIONAL) < 0) {
            raiseValidation("OTC RFQ minimum notional is " + MIN_OTC_NOTIONAL);
        }
        if (request.expiresAt().isBefore(clock.instant())) {
            raiseValidation("Expiry must be in the future");
        }
        RfqRequest rfq = rfqRequestRepository.save(RfqRequest.builder()
                .contractId(request.contractId())
                .flatId(request.flatId())
                .buildingId(request.buildingId())
                .side(request.side())
                .tokenAmount(request.tokenAmount())
                .notionalUsd(notional)
                .requesterId(request.requesterId())
                .walletAddress(request.walletAddress())
                .liquidityTier(request.liquidityTier() != null ? request.liquidityTier() : "TIER_2")
                .expiresAt(request.expiresAt())
                .build());
        return toRequestResponse(rfq);
    }

    @Transactional
    public RfqQuoteResponse submitQuote(UUID rfqId, SubmitRfqQuoteRequest request) {
        RfqRequest rfq = getOpenRequest(rfqId);
        assertWalletApproved(request.walletAddress());
        if (request.quoterId().equals(rfq.getRequesterId())) {
            raiseValidation("Cannot quote your own RFQ");
        }
        BigDecimal total = request.pricePerTokenUsd().multiply(BigDecimal.valueOf(rfq.getTokenAmount()));
        RfqQuote quote = rfqQuoteRepository.save(RfqQuote.builder()
                .rfqRequestId(rfqId)
                .quoterId(request.quoterId())
                .walletAddress(request.walletAddress())
                .pricePerTokenUsd(request.pricePerTokenUsd())
                .totalPriceUsd(total)
                .build());
        if (rfq.getStatus() == RfqRequest.RfqStatus.OPEN) {
            rfq.setStatus(RfqRequest.RfqStatus.QUOTED);
            rfqRequestRepository.save(rfq);
        }
        return toQuoteResponse(quote);
    }

    @Transactional
    public RfqRequestResponse acceptQuote(UUID rfqId, AcceptRfqQuoteRequest request) {
        RfqRequest rfq = getOpenRequest(rfqId);
        if (!rfq.getRequesterId().equals(request.requesterId())) {
            raiseValidation("Only the RFQ requester can accept a quote");
        }
        RfqQuote quote = rfqQuoteRepository.findByIdAndRfqRequestId(request.quoteId(), rfqId)
                .orElseThrow(() -> new ResourceNotFoundException("RfqQuote", request.quoteId()));
        if (quote.getStatus() != RfqQuote.QuoteStatus.PENDING) {
            throw new ConflictException("Quote is not pending");
        }

        UUID buyerId;
        UUID sellerId;
        String buyerWallet;
        String sellerWallet;
        if (rfq.getSide() == RfqRequest.RfqSide.BUY) {
            buyerId = rfq.getRequesterId();
            sellerId = quote.getQuoterId();
            buyerWallet = rfq.getWalletAddress();
            sellerWallet = quote.getWalletAddress();
        } else {
            buyerId = quote.getQuoterId();
            sellerId = rfq.getRequesterId();
            buyerWallet = quote.getWalletAddress();
            sellerWallet = rfq.getWalletAddress();
        }

        String country = assertWalletApproved(buyerWallet).countryCode();
        complianceClient.checkInvestment(buyerId, country != null ? country : "US", quote.getTotalPriceUsd());

        MarketOrder settlementOrder = marketOrderRepository.save(MarketOrder.builder()
                .listingId(null)
                .flatId(rfq.getFlatId())
                .contractId(rfq.getContractId())
                .listingType(Listing.ListingType.SECONDARY)
                .orderType(MarketOrder.OrderType.BUY)
                .status(MarketOrder.OrderStatus.MATCHED)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .buyerWallet(buyerWallet)
                .sellerWallet(sellerWallet)
                .tokenAmount(rfq.getTokenAmount())
                .totalPriceUsd(quote.getTotalPriceUsd())
                .build());

        Trade trade = tradeRepository.save(Trade.builder()
                .orderId(settlementOrder.getId())
                .listingId(null)
                .flatId(rfq.getFlatId())
                .contractId(rfq.getContractId())
                .listingType(Listing.ListingType.SECONDARY)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .sellerWallet(sellerWallet)
                .tokenAmount(rfq.getTokenAmount())
                .totalPriceUsd(quote.getTotalPriceUsd())
                .status(Trade.TradeStatus.PENDING)
                .build());

        PaymentClient.InitiatePaymentResponse payment = paymentClient.initiateTokenPurchase(
                settlementOrder.getId(), buyerId, buyerWallet, quote.getTotalPriceUsd(), sellerId);
        if (payment == null || payment.id() == null) {
            raiseValidation("Payment service returned empty response");
        }
        trade.setPaymentId(payment.id());
        tradeRepository.save(trade);

        quote.setStatus(RfqQuote.QuoteStatus.ACCEPTED);
        rfqQuoteRepository.save(quote);
        rfq.setStatus(RfqRequest.RfqStatus.ACCEPTED);
        rfq.setAcceptedQuoteId(quote.getId());
        rfqRequestRepository.save(rfq);

        orderMatchedPublisher.publishOrderMatched(new OrderMatchedPublisher.OrderMatchedEvent(
                settlementOrder.getId(),
                trade.getId(),
                null,
                rfq.getFlatId(),
                rfq.getContractId(),
                buyerId,
                sellerId,
                rfq.getTokenAmount(),
                quote.getTotalPriceUsd(),
                payment.id()));

        return toRequestResponse(rfq);
    }

    @Transactional
    public RfqRequestResponse cancel(UUID rfqId, UUID requesterId) {
        RfqRequest rfq = getRequest(rfqId);
        if (!rfq.getRequesterId().equals(requesterId)) {
            raiseValidation("Only the requester can cancel");
        }
        if (rfq.getStatus() == RfqRequest.RfqStatus.ACCEPTED) {
            throw new ConflictException("Accepted RFQ cannot be cancelled");
        }
        rfq.setStatus(RfqRequest.RfqStatus.CANCELLED);
        return toRequestResponse(rfqRequestRepository.save(rfq));
    }

    private RfqRequest getRequest(UUID id) {
        return rfqRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RfqRequest", id));
    }

    private RfqRequest getOpenRequest(UUID id) {
        RfqRequest rfq = getRequest(id);
        if (rfq.getExpiresAt().isBefore(clock.instant())) {
            rfq.setStatus(RfqRequest.RfqStatus.EXPIRED);
            rfqRequestRepository.save(rfq);
            raiseValidation("RFQ has expired");
        }
        if (rfq.getStatus() != RfqRequest.RfqStatus.OPEN
                && rfq.getStatus() != RfqRequest.RfqStatus.QUOTED) {
            raiseValidation("RFQ is not open for quotes or acceptance");
        }
        return rfq;
    }

    private ComplianceClient.ComplianceCheckResponse assertWalletApproved(String wallet) {
        ComplianceClient.ComplianceCheckResponse response = complianceClient.checkWallet(wallet);
        if (response == null || !response.whitelisted()) {
            throw new ComplianceBlockedException("Wallet is not KYC approved");
        }
        return response;
    }

    private RfqRequestResponse toRequestResponse(RfqRequest rfq) {
        return RfqRequestResponse.builder()
                .id(rfq.getId())
                .contractId(rfq.getContractId())
                .flatId(rfq.getFlatId())
                .buildingId(rfq.getBuildingId())
                .side(rfq.getSide())
                .tokenAmount(rfq.getTokenAmount())
                .notionalUsd(rfq.getNotionalUsd())
                .requesterId(rfq.getRequesterId())
                .walletAddress(rfq.getWalletAddress())
                .liquidityTier(rfq.getLiquidityTier())
                .expiresAt(rfq.getExpiresAt())
                .status(rfq.getStatus())
                .acceptedQuoteId(rfq.getAcceptedQuoteId())
                .createdAt(rfq.getCreatedAt())
                .build();
    }

    private RfqQuoteResponse toQuoteResponse(RfqQuote quote) {
        return RfqQuoteResponse.builder()
                .id(quote.getId())
                .rfqRequestId(quote.getRfqRequestId())
                .quoterId(quote.getQuoterId())
                .walletAddress(quote.getWalletAddress())
                .pricePerTokenUsd(quote.getPricePerTokenUsd())
                .totalPriceUsd(quote.getTotalPriceUsd())
                .status(quote.getStatus())
                .createdAt(quote.getCreatedAt())
                .build();
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
