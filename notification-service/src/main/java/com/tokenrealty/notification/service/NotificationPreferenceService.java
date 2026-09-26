package com.tokenrealty.notification.service;

import com.tokenrealty.notification.dto.NotificationDtos.NotificationPreferencesView;
import com.tokenrealty.notification.dto.NotificationDtos.UpdateNotificationPreferencesRequest;
import com.tokenrealty.notification.entity.NotificationPreference;
import com.tokenrealty.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferencesView get(UUID userId) {
        return repository.findByUserId(userId)
                .map(this::toView)
                .orElseGet(() -> defaults(userId));
    }

    @Transactional
    public NotificationPreferencesView update(UUID userId, UpdateNotificationPreferencesRequest request) {
        NotificationPreference preference = repository.findByUserId(userId)
                .orElseGet(() -> NotificationPreference.builder().userId(userId).build());

        preference.setEmailEnabled(request.emailEnabled());
        preference.setTradeAlerts(request.tradeAlerts());
        preference.setDividendAlerts(request.dividendAlerts());
        preference.setRentReminders(request.rentReminders());

        return toView(repository.save(preference));
    }

    private NotificationPreferencesView defaults(UUID userId) {
        return new NotificationPreferencesView(userId, true, true, true, true);
    }

    private NotificationPreferencesView toView(NotificationPreference preference) {
        return new NotificationPreferencesView(
                preference.getUserId(),
                preference.isEmailEnabled(),
                preference.isTradeAlerts(),
                preference.isDividendAlerts(),
                preference.isRentReminders());
    }
}
