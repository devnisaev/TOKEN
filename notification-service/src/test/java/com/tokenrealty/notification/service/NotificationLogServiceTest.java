package com.tokenrealty.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tokenrealty.events.kafka.KafkaJsonEvent;
import com.tokenrealty.notification.kafka.NotificationKafkaEventTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationLogService")
class NotificationLogServiceTest {

    @Mock NotificationEmailService notificationEmailService;
    @Mock NotificationPreferenceGate preferenceGate;
    @InjectMocks NotificationLogService notificationLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("logEvent sends email when preferences allow delivery")
    void logEvent_sendsWhenAllowed() {
        UUID buyerId = UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode().put("buyerId", buyerId.toString());
        KafkaJsonEvent event = mock(KafkaJsonEvent.class);
        when(event.payload()).thenReturn(payload);

        when(preferenceGate.shouldDeliver(eq(NotificationKafkaEventTypes.TRADE_SETTLED), eq(buyerId)))
                .thenReturn(true);

        notificationLogService.logEvent(NotificationKafkaEventTypes.TRADE_SETTLED, event);

        verify(notificationEmailService).send(NotificationKafkaEventTypes.TRADE_SETTLED, payload);
    }

    @Test
    @DisplayName("logEvent skips email when preferences block delivery")
    void logEvent_skipsWhenBlocked() {
        UUID buyerId = UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode().put("buyerId", buyerId.toString());
        KafkaJsonEvent event = mock(KafkaJsonEvent.class);
        when(event.payload()).thenReturn(payload);

        when(preferenceGate.shouldDeliver(eq(NotificationKafkaEventTypes.TRADE_SETTLED), eq(buyerId)))
                .thenReturn(false);

        notificationLogService.logEvent(NotificationKafkaEventTypes.TRADE_SETTLED, event);

        verify(notificationEmailService, never()).send(any(), any());
    }
}
