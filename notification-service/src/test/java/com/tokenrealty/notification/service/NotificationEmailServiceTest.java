package com.tokenrealty.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
                {"investorId":"%s","walletAddress":"0xabc","approvedAt":"2025-09-25T16:00:00Z"}
                """.formatted(UUID.randomUUID()));
        notificationEmailService.send(
                "tokenrealty.compliance.investor.kyc-approved.v1",
                payload);
    }

    @Test
    @DisplayName("buildBody renders readable KYC approved template")
    void buildBody_kycApproved() throws Exception {
        var payload = objectMapper.readTree("""
                {"investorId":"%s","walletAddress":"0xabc","approvedAt":"2025-09-25T16:00:00Z"}
                """.formatted(UUID.randomUUID()));

        String body = invokeBuildBody("tokenrealty.compliance.investor.kyc-approved.v1", payload);

        assertThat(body).contains("KYC verification has been approved");
        assertThat(body).contains("0xabc");
        assertThat(body).doesNotContain("\"investorId\"");
    }

    @Test
    @DisplayName("buildBody renders readable trade settled template")
    void buildBody_tradeSettled() throws Exception {
        var payload = objectMapper.readTree("""
                {"tradeId":"%s","orderId":"%s","listingId":"%s","paymentId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        String body = invokeBuildBody("tokenrealty.marketplace.trade.settled.v1", payload);

        assertThat(body).contains("trade has settled");
        assertThat(body).contains("Trade ID:");
    }

    @Test
    @DisplayName("buildBody renders readable order matched template")
    void buildBody_orderMatched() throws Exception {
        var payload = objectMapper.readTree("""
                {"orderId":"%s","listingId":"%s","tokenAmount":100,"totalPriceUsd":"10000.00"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));

        String body = invokeBuildBody("tokenrealty.marketplace.order.matched.v1", payload);

        assertThat(body).contains("buy order has been matched");
        assertThat(body).contains("10000.00");
    }

    @Test
    @DisplayName("buildBody renders readable rent due template")
    void buildBody_rentDue() throws Exception {
        var payload = objectMapper.readTree("""
                {"leaseId":"%s","flatId":"%s","tenantId":"%s","amountUsd":"1200.00","period":"2025-09","dueDate":"2025-09-01"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        String body = invokeBuildBody("tokenrealty.rental.rent.due.v1", payload);

        assertThat(body).contains("rent payment is due");
        assertThat(body).contains("2025-09");
    }

    @Test
    @DisplayName("buildBody renders readable dividend distributed template")
    void buildBody_dividendDistributed() throws Exception {
        var payload = objectMapper.readTree("""
                {"contractId":"%s","flatId":"%s","totalAmount":{"value":"5000.00","currency":"USDC"},"period":"2025-09","distributedAt":"2025-09-25T16:00:00Z"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));

        String body = invokeBuildBody("tokenrealty.issuance.dividend.distributed.v1", payload);

        assertThat(body).contains("dividend has been distributed");
        assertThat(body).contains("5000.00");
        assertThat(body).contains("USDC");
    }

    private String invokeBuildBody(String eventType, com.fasterxml.jackson.databind.JsonNode payload)
            throws Exception {
        Method method = NotificationEmailService.class.getDeclaredMethod(
                "buildBody", String.class, com.fasterxml.jackson.databind.JsonNode.class);
        method.setAccessible(true);
        return (String) method.invoke(notificationEmailService, eventType, payload);
    }
}
