package com.tokenrealty.notification.service;

import com.tokenrealty.notification.kafka.NotificationKafkaEventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceGate {

    private final NotificationPreferenceService preferenceService;

    public boolean shouldDeliver(String eventType, UUID userId) {
        if (userId == null) {
            return true;
        }
        var preferences = preferenceService.get(userId);
        if (!preferences.emailEnabled()) {
            return false;
        }
        return switch (category(eventType)) {
            case TRADE -> preferences.tradeAlerts();
            case DIVIDEND -> preferences.dividendAlerts();
            case RENT -> preferences.rentReminders();
            case GENERAL -> true;
        };
    }

    private enum AlertCategory {
        TRADE, DIVIDEND, RENT, GENERAL
    }

    private AlertCategory category(String eventType) {
        return switch (eventType) {
            case NotificationKafkaEventTypes.ORDER_MATCHED,
                 NotificationKafkaEventTypes.TRADE_SETTLED,
                 NotificationKafkaEventTypes.PAYMENT_CONFIRMED,
                 NotificationKafkaEventTypes.TRANSFER_COMPLETED,
                 NotificationKafkaEventTypes.LISTING_CREATED -> AlertCategory.TRADE;
            case NotificationKafkaEventTypes.DIVIDEND_DISTRIBUTED -> AlertCategory.DIVIDEND;
            case NotificationKafkaEventTypes.RENT_DUE,
                 NotificationKafkaEventTypes.RENT_COLLECTED,
                 NotificationKafkaEventTypes.LEASE_EXPIRED -> AlertCategory.RENT;
            default -> AlertCategory.GENERAL;
        };
    }
}
