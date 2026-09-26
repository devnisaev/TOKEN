package com.tokenrealty.notification.controller;

import com.tokenrealty.notification.dto.NotificationDtos.SendNotificationRequest;
import com.tokenrealty.notification.dto.NotificationDtos.SendNotificationResponse;
import com.tokenrealty.notification.service.NotificationSendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationSendService notificationSendService;

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public SendNotificationResponse send(@Valid @RequestBody SendNotificationRequest request) {
        notificationSendService.sendManual(request.eventType(), request.payload());
        return new SendNotificationResponse(request.eventType(), "queued");
    }
}
