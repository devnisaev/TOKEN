package com.tokenrealty.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.notification.dto.NotificationDtos.UpdateNotificationPreferencesRequest;
import com.tokenrealty.notification.kafka.NotificationKafkaEventTypes;
import com.tokenrealty.notification.service.NotificationLogService;
import com.tokenrealty.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Notification Kafka integration test")
class NotificationKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired NotificationLogService notificationLogService;
    @Autowired NotificationPreferenceService preferenceService;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean com.tokenrealty.notification.service.NotificationEmailService notificationEmailService;

    @Test
    @DisplayName("trade.settled sends email when trade alerts enabled")
    void tradeSettled_sendsWhenAllowed() throws Exception {
        UUID buyerId = UUID.randomUUID();
        preferenceService.update(buyerId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestTradeSettled(buyerId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.TRADE_SETTLED), any());
    }

    @Test
    @DisplayName("trade.settled skips email when trade alerts disabled")
    void tradeSettled_skipsWhenTradeAlertsDisabled() throws Exception {
        UUID buyerId = UUID.randomUUID();
        preferenceService.update(buyerId, new UpdateNotificationPreferencesRequest(true, false, true, true));

        ingestTradeSettled(buyerId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("rent.due sends email when rent reminders enabled")
    void rentDue_sendsWhenAllowed() throws Exception {
        UUID tenantId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestRentDue(tenantId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.RENT_DUE), any());
    }

    @Test
    @DisplayName("rent.due skips email when rent reminders disabled")
    void rentDue_skipsWhenRentRemindersDisabled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, false));

        ingestRentDue(tenantId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("rent.due dedupes duplicate eventId")
    void rentDue_dedupesDuplicateEventId() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestRentDue(tenantId, eventId);
        ingestRentDue(tenantId, eventId);

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.RENT_DUE), any());
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void tradeSettled_dedupesDuplicateEventId() throws Exception {
        UUID buyerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        preferenceService.update(buyerId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestTradeSettled(buyerId, eventId);
        ingestTradeSettled(buyerId, eventId);

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.TRADE_SETTLED), any());
    }

    private void ingestRentDue(UUID tenantId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("leaseId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("amountUsd", "850.00");
        payload.put("period", "2025-09");
        payload.put("dueDate", "2025-09-01");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.RENT_DUE,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.RENT_DUE, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.RENT_DUE, event));
    }

    private void ingestTradeSettled(UUID buyerId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buyerId", buyerId.toString());
        payload.put("orderId", UUID.randomUUID().toString());
        payload.put("tradeId", UUID.randomUUID().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.TRADE_SETTLED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.TRADE_SETTLED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.TRADE_SETTLED, event));
    }
}
