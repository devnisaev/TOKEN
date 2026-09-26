package com.tokenrealty.notification.service;

import com.tokenrealty.notification.dto.NotificationDtos.UpdateNotificationPreferencesRequest;
import com.tokenrealty.notification.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("NotificationPreferenceService")
class NotificationPreferenceServiceTest {

    @Autowired NotificationPreferenceService preferenceService;
    @Autowired NotificationPreferenceRepository repository;

    @Test
    @DisplayName("update persists preferences and get returns saved values")
    void update_persistsPreferences() {
        UUID userId = UUID.randomUUID();

        var defaults = preferenceService.get(userId);
        assertThat(defaults.emailEnabled()).isTrue();

        var updated = preferenceService.update(userId, new UpdateNotificationPreferencesRequest(
                false, true, false, true));

        assertThat(updated.emailEnabled()).isFalse();
        assertThat(updated.dividendAlerts()).isFalse();
        assertThat(repository.findByUserId(userId)).isPresent();
        assertThat(preferenceService.get(userId).emailEnabled()).isFalse();
    }
}
