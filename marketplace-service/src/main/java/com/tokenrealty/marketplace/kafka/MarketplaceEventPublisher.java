package com.tokenrealty.marketplace.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import com.tokenrealty.marketplace.kafka.outbox.OutboxEvent;
import com.tokenrealty.marketplace.kafka.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketplaceEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${tokenrealty.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public void publishListingCreated(Listing listing) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listingId", listing.getId());
        payload.put("flatId", listing.getFlatId());
        payload.put("listingType", listing.getListingType().name());
        payload.put("priceUsd", listing.getPriceUsd().toPlainString());
        payload.put("tokensAvailable", listing.getTokensAvailable());
        enqueue(MarketplaceKafkaEventTypes.LISTING_CREATED, listing.getId(), payload);
    }

    public void publishOrderMatched(MarketOrder order, Trade trade) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("tradeId", trade.getId());
        payload.put("listingId", order.getListingId());
        payload.put("flatId", order.getFlatId());
        payload.put("contractId", order.getContractId());
        payload.put("buyerId", order.getBuyerId());
        payload.put("sellerId", order.getSellerId());
        payload.put("tokenAmount", order.getTokenAmount());
        payload.put("totalPriceUsd", order.getTotalPriceUsd().toPlainString());
        enqueue(MarketplaceKafkaEventTypes.ORDER_MATCHED, order.getId(), payload);
    }

    public void publishTradeSettled(Trade trade) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeId", trade.getId());
        payload.put("orderId", trade.getOrderId());
        payload.put("listingId", trade.getListingId());
        payload.put("paymentId", trade.getPaymentId());
        payload.put("transferId", trade.getTransferId());
        enqueue(MarketplaceKafkaEventTypes.TRADE_SETTLED, trade.getId(), payload);
    }

    private void enqueue(String eventType, UUID aggregateId, Map<String, Object> payload) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled — skipping outbox enqueue for {}", eventType);
            return;
        }
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("marketplace")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .traceId(UUID.randomUUID().toString())
                    .build();
            outboxRepository.save(event);
        } catch (Exception ex) {
            log.warn("Failed to enqueue outbox event {}: {}", eventType, ex.getMessage());
        }
    }
}
