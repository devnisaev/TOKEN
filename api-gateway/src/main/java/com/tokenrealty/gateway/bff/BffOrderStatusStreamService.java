package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.MarketplaceClient;
import com.tokenrealty.gateway.dto.BffDtos.OrderStatusEvent;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BffOrderStatusStreamService {

    private static final long TIMEOUT_MS = 300_000L;
    private static final long POLL_MS = 2_000L;
    private static final long HEARTBEAT_MS = 15_000L;

    private final MarketplaceClient marketplaceClient;

    public SseEmitter stream(UUID orderId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Thread.startVirtualThread(() -> poll(orderId, emitter));
        return emitter;
    }

    private void poll(UUID orderId, SseEmitter emitter) {
        OrderStatusEvent lastSent = null;
        long lastHeartbeat = System.currentTimeMillis();
        try {
            while (true) {
                OrderStatusEvent event = loadSnapshot(orderId);
                if (!event.equals(lastSent)) {
                    emitter.send(SseEmitter.event().name("status").data(event));
                    lastSent = event;
                    if (isTerminal(event)) {
                        emitter.complete();
                        return;
                    }
                } else if (System.currentTimeMillis() - lastHeartbeat >= HEARTBEAT_MS) {
                    emitter.send(SseEmitter.event().comment("ping"));
                    lastHeartbeat = System.currentTimeMillis();
                }
                Thread.sleep(POLL_MS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            emitter.completeWithError(ex);
        } catch (ResourceNotFoundException ex) {
            try {
                emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
            } catch (IOException ignored) {
                /* client disconnected */
            }
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    private OrderStatusEvent loadSnapshot(UUID orderId) {
        var order = marketplaceClient.getOrder(orderId);
        var trade = marketplaceClient.getTrade(orderId);
        return new OrderStatusEvent(
                orderId,
                order.status(),
                trade != null ? trade.status() : null);
    }

    private static boolean isTerminal(OrderStatusEvent event) {
        if ("SETTLED".equals(event.orderStatus()) || "CANCELLED".equals(event.orderStatus())) {
            return true;
        }
        return "SETTLED".equals(event.tradeStatus()) || "FAILED".equals(event.tradeStatus());
    }
}
