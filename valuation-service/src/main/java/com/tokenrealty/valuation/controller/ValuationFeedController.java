package com.tokenrealty.valuation.controller;

import com.tokenrealty.valuation.dto.ValuationDtos.ValuationRequestResponse;
import com.tokenrealty.valuation.service.ValuationFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/valuations/feeds")
@RequiredArgsConstructor
@Tag(name = "Valuation Feeds", description = "AVM and market-data feed ingest")
public class ValuationFeedController {

    private final ValuationFeedService valuationFeedService;

    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest external valuation feed payload")
    public ValuationRequestResponse ingest(
            @PathVariable String provider,
            @RequestBody String rawBody) {
        return valuationFeedService.ingestFeed(provider, rawBody);
    }
}
