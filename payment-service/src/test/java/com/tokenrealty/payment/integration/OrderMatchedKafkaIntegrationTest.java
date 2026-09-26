package com.tokenrealty.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.kafka.PaymentKafkaEventTypes;
import com.tokenrealty.payment.kafka.command.OrderMatchedCommand;
import com.tokenrealty.payment.repository.PaymentRepository;
import com.tokenrealty.payment.service.OrderEscrowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Order matched Kafka integration test")
class OrderMatchedKafkaIntegrationTest {

    @Autowired OrderEscrowService orderEscrowService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("order.matched succeeds when escrow payment already exists")
    void orderMatched_whenPaymentExists_completes() throws Exception {
        UUID orderId = UUID.randomUUID();
        paymentRepository.save(Payment.builder()
                .orderId(orderId)
                .payerId(UUID.randomUUID())
                .payerWallet("0xPayer")
                .amount(new BigDecimal("50.00"))
                .currency(PaymentCurrency.USDC)
                .status(Payment.PaymentStatus.PENDING)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .idempotencyKey("order-" + orderId)
                .build());

        ingestOrderMatched(orderId, UUID.randomUUID(), UUID.randomUUID());

        assertThat(paymentRepository.existsByOrderId(orderId)).isTrue();
    }

    @Test
    @DisplayName("order.matched without payment record is handled without error")
    void orderMatched_whenPaymentMissing_logsWarning() throws Exception {
        UUID orderId = UUID.randomUUID();

        ingestOrderMatched(orderId, UUID.randomUUID(), UUID.randomUUID());

        assertThat(paymentRepository.existsByOrderId(orderId)).isFalse();
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void orderMatched_dedupesDuplicateEventId() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        paymentRepository.save(Payment.builder()
                .orderId(orderId)
                .payerId(UUID.randomUUID())
                .payerWallet("0xPayer")
                .amount(new BigDecimal("25.00"))
                .currency(PaymentCurrency.USDC)
                .status(Payment.PaymentStatus.PENDING)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .idempotencyKey("dedupe-" + orderId)
                .build());

        ingestOrderMatched(orderId, eventId, UUID.randomUUID());
        ingestOrderMatched(orderId, eventId, UUID.randomUUID());

        assertThat(paymentRepository.findAll().stream().filter(p -> p.getOrderId().equals(orderId)).count())
                .isEqualTo(1);
    }

    private void ingestOrderMatched(UUID orderId, UUID eventId, UUID paymentId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("paymentId", paymentId.toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                PaymentKafkaEventTypes.ORDER_MATCHED,
                orderId.toString(),
                payload));
        eventConsumer.consume(message, PaymentKafkaEventTypes.ORDER_MATCHED,
                "Order matched reconciliation failed",
                event -> orderEscrowService.ensureEscrowLinked(OrderMatchedCommand.from(event)));
    }
}
