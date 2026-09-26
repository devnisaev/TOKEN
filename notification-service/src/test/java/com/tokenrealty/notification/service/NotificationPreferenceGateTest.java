package com.tokenrealty.notification.service;

import com.tokenrealty.notification.dto.NotificationDtos.NotificationPreferencesView;
import com.tokenrealty.notification.kafka.NotificationKafkaEventTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferenceGate")
class NotificationPreferenceGateTest {

    @Mock NotificationPreferenceService preferenceService;
    @InjectMocks NotificationPreferenceGate preferenceGate;

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("allows delivery when userId is absent")
    void shouldDeliver_withoutUserId() {
        assertThat(preferenceGate.shouldDeliver(NotificationKafkaEventTypes.TRADE_SETTLED, null))
                .isTrue();
    }

    @Test
    @DisplayName("blocks delivery when email is disabled")
    void shouldDeliver_emailDisabled() {
        when(preferenceService.get(userId)).thenReturn(preferences(false, true, true, true));

        assertThat(preferenceGate.shouldDeliver(NotificationKafkaEventTypes.TRADE_SETTLED, userId))
                .isFalse();
    }

    @Test
    @DisplayName("blocks trade alerts when tradeAlerts is false")
    void shouldDeliver_tradeAlertsDisabled() {
        when(preferenceService.get(userId)).thenReturn(preferences(true, false, true, true));

        assertThat(preferenceGate.shouldDeliver(NotificationKafkaEventTypes.ORDER_MATCHED, userId))
                .isFalse();
    }

    @Test
    @DisplayName("allows dividend alerts when dividendAlerts is true")
    void shouldDeliver_dividendAlertsEnabled() {
        when(preferenceService.get(userId)).thenReturn(preferences(true, true, true, true));

        assertThat(preferenceGate.shouldDeliver(NotificationKafkaEventTypes.DIVIDEND_DISTRIBUTED, userId))
                .isTrue();
    }

    @Test
    @DisplayName("blocks rent reminders when rentReminders is false")
    void shouldDeliver_rentRemindersDisabled() {
        when(preferenceService.get(userId)).thenReturn(preferences(true, true, true, false));

        assertThat(preferenceGate.shouldDeliver(NotificationKafkaEventTypes.RENT_DUE, userId))
                .isFalse();
    }

    @Test
    @DisplayName("allows general alerts when email is enabled")
    void shouldDeliver_generalAlerts() {
        when(preferenceService.get(userId)).thenReturn(preferences(true, false, false, false));

        assertThat(preferenceGate.shouldDeliver(NotificationKafkaEventTypes.KYC_APPROVED, userId))
                .isTrue();
    }

    private NotificationPreferencesView preferences(
            boolean emailEnabled,
            boolean tradeAlerts,
            boolean dividendAlerts,
            boolean rentReminders) {
        return new NotificationPreferencesView(
                userId, emailEnabled, tradeAlerts, dividendAlerts, rentReminders);
    }
}
