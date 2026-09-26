package com.tokenrealty.notification.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

final class NotificationPayloadUsers {

    private static final List<String> USER_ID_FIELDS = List.of(
            "userId", "investorId", "buyerId", "payerId", "tenantId", "sellerId");

    private NotificationPayloadUsers() {
    }

    static UUID resolveUserId(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        for (String field : USER_ID_FIELDS) {
            UUID userId = parseUuid(payload.get(field));
            if (userId != null) {
                return userId;
            }
        }
        return null;
    }

    private static UUID parseUuid(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return UUID.fromString(node.asText());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
