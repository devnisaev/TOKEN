package com.tokenrealty.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tokenrealty.events.kafka.KafkaJsonEvent;
import com.tokenrealty.reporting.entity.DividendRecord;
import com.tokenrealty.reporting.entity.FlatTokenizedRecord;
import com.tokenrealty.reporting.entity.OrderMatchedRecord;
import com.tokenrealty.reporting.entity.RentCollectedRecord;
import com.tokenrealty.reporting.entity.TradeSettledRecord;
import com.tokenrealty.reporting.repository.DividendRecordRepository;
import com.tokenrealty.reporting.repository.FlatTokenizedRecordRepository;
import com.tokenrealty.reporting.repository.OrderMatchedRecordRepository;
import com.tokenrealty.reporting.repository.RentCollectedRecordRepository;
import com.tokenrealty.reporting.repository.TradeSettledRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportingProjectionService {

    private final TradeSettledRecordRepository tradeSettledRecordRepository;
    private final OrderMatchedRecordRepository orderMatchedRecordRepository;
    private final DividendRecordRepository dividendRecordRepository;
    private final RentCollectedRecordRepository rentCollectedRecordRepository;
    private final FlatTokenizedRecordRepository flatTokenizedRecordRepository;

    @Transactional
    public void onTradeSettled(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        tradeSettledRecordRepository.save(TradeSettledRecord.builder()
                .sourceEventId(event.eventId())
                .tradeId(uuid(payload, "tradeId"))
                .orderId(uuid(payload, "orderId"))
                .listingId(uuid(payload, "listingId"))
                .paymentId(uuid(payload, "paymentId"))
                .transferId(uuid(payload, "transferId"))
                .settledAt(event.occurredAt())
                .build());
    }

    @Transactional
    public void onOrderMatched(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        orderMatchedRecordRepository.save(OrderMatchedRecord.builder()
                .sourceEventId(event.eventId())
                .orderId(uuid(payload, "orderId"))
                .listingId(uuid(payload, "listingId"))
                .flatId(uuid(payload, "flatId"))
                .contractId(uuid(payload, "contractId"))
                .buyerId(uuid(payload, "buyerId"))
                .sellerId(uuid(payload, "sellerId"))
                .tokenAmount(longValue(payload, "tokenAmount"))
                .totalPriceUsd(decimal(payload, "totalPriceUsd"))
                .matchedAt(event.occurredAt())
                .build());
    }

    @Transactional
    public void onDividendDistributed(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        JsonNode totalAmount = payload.path("totalAmount");
        BigDecimal amount = totalAmount.isMissingNode()
                ? decimal(payload, "totalAmountUsd")
                : decimal(totalAmount, "value");
        JsonNode holderPayouts = payload.path("holderPayouts");
        int holderCount = holderPayouts.isArray() ? holderPayouts.size() : 0;

        dividendRecordRepository.save(DividendRecord.builder()
                .sourceEventId(event.eventId())
                .contractId(uuid(payload, "contractId"))
                .flatId(uuid(payload, "flatId"))
                .period(text(payload, "period"))
                .totalAmountUsd(amount)
                .holderCount(holderCount)
                .distributedAt(parseInstant(payload, "distributedAt", event.occurredAt()))
                .build());
    }

    @Transactional
    public void onRentCollected(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        JsonNode amount = payload.path("amount");
        BigDecimal amountUsd = amount.isMissingNode()
                ? decimal(payload, "amountUsd")
                : decimal(amount, "value");

        rentCollectedRecordRepository.save(RentCollectedRecord.builder()
                .sourceEventId(event.eventId())
                .leaseId(uuid(payload, "leaseId"))
                .flatId(uuid(payload, "flatId"))
                .tenantId(uuid(payload, "tenantId"))
                .period(text(payload, "period"))
                .amountUsd(amountUsd)
                .collectedAt(parseInstant(payload, "collectedAt", event.occurredAt()))
                .build());
    }

    @Transactional
    public void onFlatTokenized(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        flatTokenizedRecordRepository.save(FlatTokenizedRecord.builder()
                .sourceEventId(event.eventId())
                .flatId(uuid(payload, "flatId"))
                .buildingId(uuid(payload, "buildingId"))
                .contractAddress(text(payload, "contractAddress"))
                .totalTokens(longValue(payload, "totalTokens"))
                .tokenPriceUsd(decimal(payload, "tokenPriceUsd"))
                .tokenizedAt(event.occurredAt())
                .build());
    }

    private static UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : UUID.fromString(value);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asLong();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : new BigDecimal(value);
    }

    private static Instant parseInstant(JsonNode node, String field, Instant fallback) {
        String value = text(node, field);
        return value == null ? fallback : Instant.parse(value);
    }
}
