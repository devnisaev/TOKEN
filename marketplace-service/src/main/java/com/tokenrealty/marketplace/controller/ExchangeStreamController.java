package com.tokenrealty.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.BookDepthResponse;
import com.tokenrealty.marketplace.service.ExchangeMarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/v1/exchange")
@RequiredArgsConstructor
public class ExchangeStreamController {

    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final long POLL_INTERVAL_MS = 2_000L;

    private final ExchangeMarketDataService exchangeMarketDataService;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/book/{contractId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBook(@PathVariable UUID contractId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                BookDepthResponse book = exchangeMarketDataService.getBook(contractId);
                emitter.send(SseEmitter.event()
                        .name("book")
                        .data(objectMapper.writeValueAsString(book)));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
                scheduler.shutdown();
            } catch (RuntimeException ex) {
                emitter.completeWithError(ex);
                scheduler.shutdown();
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        emitter.onCompletion(scheduler::shutdown);
        emitter.onTimeout(scheduler::shutdown);
        emitter.onError(error -> scheduler.shutdown());
        return emitter;
    }
}
