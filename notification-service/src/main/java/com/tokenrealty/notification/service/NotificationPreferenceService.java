package com.tokenrealty.notification.service;

import com.tokenrealty.notification.dto.NotificationDtos.NotificationPreferencesView;
import com.tokenrealty.notification.dto.NotificationDtos.UpdateNotificationPreferencesRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationPreferenceService {

    private final Map<UUID, NotificationPreferencesView> store = new ConcurrentHashMap<>();

    public NotificationPreferencesView get(UUID userId) {
        return store.getOrDefault(userId, defaults(userId));
    }

    public NotificationPreferencesView update(UUID userId, UpdateNotificationPreferencesRequest request) {
        NotificationPreferencesView updated = new NotificationPreferencesView(
                userId,
                request.emailEnabled(),
                request.tradeAlerts(),
                request.dividendAlerts(),
                request.rentReminders());
        store.put(userId, updated);
        return updated;
    }

    private NotificationPreferencesView defaults(UUID userId) {
        return new NotificationPreferencesView(userId, true, true, true, true);
    }
}
