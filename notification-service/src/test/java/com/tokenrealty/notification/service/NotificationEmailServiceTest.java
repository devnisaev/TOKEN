package com.tokenrealty.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "tokenrealty.notification.email.enabled=true",
        "tokenrealty.notification.email.mode=log"
})
class NotificationEmailServiceTest {

    @Autowired
    NotificationEmailService notificationEmailService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("send logs email in log mode")
    void sendInLogMode() throws Exception {
        var payload = objectMapper.readTree("""
                {"investorId":"%s","walletAddress":"0xabc"}
                """.formatted(UUID.randomUUID()));
        notificationEmailService.send(
                "tokenrealty.compliance.investor.kyc-approved.v1",
                payload);
    }
}
