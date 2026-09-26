package com.tokenrealty.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSendService unit tests")
class NotificationSendServiceTest {

    @Mock NotificationEmailService notificationEmailService;
    @InjectMocks NotificationSendService notificationSendService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("sendManual delegates to NotificationEmailService")
    void sendManual_delegatesToEmailService() {
        var payload = objectMapper.createObjectNode().put("investorId", "abc-123");

        notificationSendService.sendManual("tokenrealty.test.event.v1", payload);

        verify(notificationEmailService).send("tokenrealty.test.event.v1", payload);
    }
}
