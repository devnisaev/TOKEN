package com.tokenrealty.notification.controller;

import com.tokenrealty.notification.dto.NotificationDtos.NotificationPreferencesView;
import com.tokenrealty.notification.dto.NotificationDtos.SendNotificationRequest;
import com.tokenrealty.notification.dto.NotificationDtos.SendNotificationResponse;
import com.tokenrealty.notification.dto.NotificationDtos.UpdateNotificationPreferencesRequest;
import com.tokenrealty.notification.service.NotificationPreferenceService;
import com.tokenrealty.notification.service.NotificationSendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSendService notificationSendService;
    private final NotificationPreferenceService preferenceService;

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public SendNotificationResponse send(@Valid @RequestBody SendNotificationRequest request) {
        notificationSendService.sendManual(request.eventType(), request.payload());
        return new SendNotificationResponse(request.eventType(), "queued");
    }

    @GetMapping("/preferences/{userId}")
    public NotificationPreferencesView getPreferences(@PathVariable UUID userId) {
        return preferenceService.get(userId);
    }

    @PatchMapping("/preferences/{userId}")
    public NotificationPreferencesView updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request) {
        return preferenceService.update(userId, request);
    }
}
