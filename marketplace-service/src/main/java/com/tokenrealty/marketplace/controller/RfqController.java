package com.tokenrealty.marketplace.controller;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.service.MarketMakerService;
import com.tokenrealty.marketplace.service.RfqService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/exchange")
@RequiredArgsConstructor
@Tag(name = "OTC / RFQ", description = "Block trade RFQ desk (Phase 15)")
public class RfqController {

    private final RfqService rfqService;
    private final MarketMakerService marketMakerService;

    @GetMapping("/rfq")
    @Operation(summary = "List open RFQ requests")
    public Page<RfqRequestResponse> listOpen(@PageableDefault(size = 20) Pageable pageable) {
        return rfqService.listOpen(pageable);
    }

    @GetMapping("/rfq/mine")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "List RFQ requests for an investor")
    public Page<RfqRequestResponse> listMine(
            @RequestParam UUID requesterId,
            @PageableDefault(size = 20) Pageable pageable) {
        return rfqService.listByRequester(requesterId, pageable);
    }

    @GetMapping("/rfq/{id}")
    @Operation(summary = "Get RFQ request by id")
    public RfqRequestResponse getById(@PathVariable UUID id) {
        return rfqService.findById(id);
    }

    @PostMapping("/rfq")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Create an OTC RFQ (min $500k notional)")
    public RfqRequestResponse create(@Valid @RequestBody CreateRfqRequest request) {
        return rfqService.createRequest(request);
    }

    @PostMapping("/rfq/{id}/quote")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Submit a quote on an RFQ")
    public RfqQuoteResponse quote(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitRfqQuoteRequest request) {
        return rfqService.submitQuote(id, request);
    }

    @PostMapping("/rfq/{id}/accept")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Accept a quote and initiate settlement")
    public RfqRequestResponse accept(
            @PathVariable UUID id,
            @Valid @RequestBody AcceptRfqQuoteRequest request) {
        return rfqService.acceptQuote(id, request);
    }

    @DeleteMapping("/rfq/{id}")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Cancel an open RFQ")
    public RfqRequestResponse cancel(
            @PathVariable UUID id,
            @RequestParam UUID requesterId) {
        return rfqService.cancel(id, requesterId);
    }

    @GetMapping("/rfq/{id}/quotes")
    @Operation(summary = "List quotes for an RFQ")
    public List<RfqQuoteResponse> listQuotes(@PathVariable UUID id) {
        return rfqService.listQuotes(id);
    }

    @PostMapping("/mm/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Market maker bulk limit order entry")
    public BulkExchangeOrderResponse submitBulkQuotes(@Valid @RequestBody BulkExchangeOrderRequest request) {
        return marketMakerService.submitBulkQuotes(request);
    }
}
