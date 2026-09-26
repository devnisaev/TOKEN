package com.tokenrealty.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record SendNotificationRequest(
            @NotBlank String eventType,
            @NotNull JsonNode payload
    ) {
    }

    public record SendNotificationResponse(
            String eventType,
            String status
    ) {
    }
}
