package com.tokenrealty.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.notification.dto.NotificationDtos.UpdateNotificationPreferencesRequest;
import com.tokenrealty.notification.kafka.NotificationKafkaEventTypes;
import com.tokenrealty.notification.service.NotificationLogService;
import com.tokenrealty.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Notification Kafka integration test")
class NotificationKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired NotificationLogService notificationLogService;
    @Autowired NotificationPreferenceService preferenceService;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean com.tokenrealty.notification.service.NotificationEmailService notificationEmailService;

    @Test
    @DisplayName("trade.settled sends email when trade alerts enabled")
    void tradeSettled_sendsWhenAllowed() throws Exception {
        UUID buyerId = UUID.randomUUID();
        preferenceService.update(buyerId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestTradeSettled(buyerId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.TRADE_SETTLED), any());
    }

    @Test
    @DisplayName("trade.settled skips email when trade alerts disabled")
    void tradeSettled_skipsWhenTradeAlertsDisabled() throws Exception {
        UUID buyerId = UUID.randomUUID();
        preferenceService.update(buyerId, new UpdateNotificationPreferencesRequest(true, false, true, true));

        ingestTradeSettled(buyerId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("rent.due sends email when rent reminders enabled")
    void rentDue_sendsWhenAllowed() throws Exception {
        UUID tenantId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestRentDue(tenantId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.RENT_DUE), any());
    }

    @Test
    @DisplayName("rent.due skips email when rent reminders disabled")
    void rentDue_skipsWhenRentRemindersDisabled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, false));

        ingestRentDue(tenantId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("rent.due dedupes duplicate eventId")
    void rentDue_dedupesDuplicateEventId() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestRentDue(tenantId, eventId);
        ingestRentDue(tenantId, eventId);

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.RENT_DUE), any());
    }

    @Test
    @DisplayName("rent.collected sends email when rent reminders enabled")
    void rentCollected_sendsWhenAllowed() throws Exception {
        UUID tenantId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestRentCollected(tenantId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.RENT_COLLECTED), any());
    }

    @Test
    @DisplayName("rent.collected skips email when rent reminders disabled")
    void rentCollected_skipsWhenRentRemindersDisabled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, false));

        ingestRentCollected(tenantId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("rent.collected dedupes duplicate eventId")
    void rentCollected_dedupesDuplicateEventId() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestRentCollected(tenantId, eventId);
        ingestRentCollected(tenantId, eventId);

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.RENT_COLLECTED), any());
    }

    @Test
    @DisplayName("building.approved sends email")
    void buildingApproved_sendsEmail() throws Exception {
        ingestBuildingApproved(UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.BUILDING_APPROVED), any());
    }

    @Test
    @DisplayName("lease.expired sends email when rent reminders enabled")
    void leaseExpired_sendsWhenAllowed() throws Exception {
        UUID tenantId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestLeaseExpired(tenantId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.LEASE_EXPIRED), any());
    }

    @Test
    @DisplayName("lease.expired skips email when rent reminders disabled")
    void leaseExpired_skipsWhenRentRemindersDisabled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        preferenceService.update(tenantId, new UpdateNotificationPreferencesRequest(true, true, true, false));

        ingestLeaseExpired(tenantId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("kyc-revoked sends email when email enabled")
    void kycRevoked_sendsWhenAllowed() throws Exception {
        UUID investorId = UUID.randomUUID();
        preferenceService.update(investorId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestKycRevoked(investorId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.KYC_REVOKED), any());
    }

    @Test
    @DisplayName("kyc-revoked skips email when email disabled")
    void kycRevoked_skipsWhenEmailDisabled() throws Exception {
        UUID investorId = UUID.randomUUID();
        preferenceService.update(investorId, new UpdateNotificationPreferencesRequest(false, true, true, true));

        ingestKycRevoked(investorId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("listing.created sends email")
    void listingCreated_sendsEmail() throws Exception {
        ingestListingCreated(UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.LISTING_CREATED), any());
    }

    @Test
    @DisplayName("payment.confirmed sends email when trade alerts enabled")
    void paymentConfirmed_sendsWhenAllowed() throws Exception {
        UUID payerId = UUID.randomUUID();
        preferenceService.update(payerId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestPaymentConfirmed(payerId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.PAYMENT_CONFIRMED), any());
    }

    @Test
    @DisplayName("payment.confirmed skips email when trade alerts disabled")
    void paymentConfirmed_skipsWhenTradeAlertsDisabled() throws Exception {
        clearInvocations(notificationEmailService);
        UUID payerId = UUID.randomUUID();
        preferenceService.update(payerId, new UpdateNotificationPreferencesRequest(true, false, true, true));

        ingestPaymentConfirmed(payerId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("transfer.completed sends email")
    void transferCompleted_sendsEmail() throws Exception {
        ingestTransferCompleted(UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.TRANSFER_COMPLETED), any());
    }

    @Test
    @DisplayName("kyc-approved sends email when email enabled")
    void kycApproved_sendsWhenAllowed() throws Exception {
        UUID investorId = UUID.randomUUID();
        preferenceService.update(investorId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestKycApproved(investorId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.KYC_APPROVED), any());
    }

    @Test
    @DisplayName("kyc-approved skips email when email disabled")
    void kycApproved_skipsWhenEmailDisabled() throws Exception {
        clearInvocations(notificationEmailService);
        UUID investorId = UUID.randomUUID();
        preferenceService.update(investorId, new UpdateNotificationPreferencesRequest(false, true, true, true));

        ingestKycApproved(investorId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("dividend.distributed sends email when dividend alerts enabled")
    void dividendDistributed_sendsWhenAllowed() throws Exception {
        UUID investorId = UUID.randomUUID();
        preferenceService.update(investorId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestDividendDistributed(UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.DIVIDEND_DISTRIBUTED), any());
    }

    @Test
    @DisplayName("flat.tokenized sends email")
    void flatTokenized_sendsEmail() throws Exception {
        ingestFlatTokenized(UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.FLAT_TOKENIZED), any());
    }

    @Test
    @DisplayName("order.matched sends email when trade alerts enabled")
    void orderMatched_sendsWhenAllowed() throws Exception {
        UUID buyerId = UUID.randomUUID();
        preferenceService.update(buyerId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestOrderMatched(buyerId, UUID.randomUUID());

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.ORDER_MATCHED), any());
    }

    @Test
    @DisplayName("order.matched skips email when trade alerts disabled")
    void orderMatched_skipsWhenTradeAlertsDisabled() throws Exception {
        clearInvocations(notificationEmailService);
        UUID buyerId = UUID.randomUUID();
        preferenceService.update(buyerId, new UpdateNotificationPreferencesRequest(true, false, true, true));

        ingestOrderMatched(buyerId, UUID.randomUUID());

        verify(notificationEmailService, never()).send(any(), any());
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void tradeSettled_dedupesDuplicateEventId() throws Exception {
        UUID buyerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        preferenceService.update(buyerId, new UpdateNotificationPreferencesRequest(true, true, true, true));

        ingestTradeSettled(buyerId, eventId);
        ingestTradeSettled(buyerId, eventId);

        verify(notificationEmailService).send(eq(NotificationKafkaEventTypes.TRADE_SETTLED), any());
    }

    private void ingestBuildingApproved(UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", UUID.randomUUID().toString());
        payload.put("approvedAt", java.time.Instant.now().toString());
        payload.put("approvedBy", UUID.randomUUID().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.BUILDING_APPROVED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.BUILDING_APPROVED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.BUILDING_APPROVED, event));
    }

    private void ingestLeaseExpired(UUID tenantId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("leaseId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("endDate", "2025-08-31");
        payload.put("expiredAt", java.time.Instant.now().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.LEASE_EXPIRED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.LEASE_EXPIRED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.LEASE_EXPIRED, event));
    }

    private void ingestKycRevoked(UUID investorId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("investorId", investorId.toString());
        payload.put("walletAddress", "0x70997970C51812dc3A010C724d1AfE6fc599aa84");
        payload.put("reason", "expired");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.KYC_REVOKED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.KYC_REVOKED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.KYC_REVOKED, event));
    }

    private void ingestRentCollected(UUID tenantId, UUID eventId) throws Exception {
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("value", "650.00");
        amount.put("currency", "USDC");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("leaseId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("period", "2025-09");
        payload.put("amount", amount);
        payload.put("txHash", "0xRentCollected");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.RENT_COLLECTED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.RENT_COLLECTED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.RENT_COLLECTED, event));
    }

    private void ingestRentDue(UUID tenantId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId.toString());
        payload.put("leaseId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("amountUsd", "850.00");
        payload.put("period", "2025-09");
        payload.put("dueDate", "2025-09-01");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.RENT_DUE,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.RENT_DUE, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.RENT_DUE, event));
    }

    private void ingestKycApproved(UUID investorId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("investorId", investorId.toString());
        payload.put("walletAddress", "0x70997970C51812dc3A010C724d1AfE6fc599aa84");
        payload.put("countryCode", "US");
        payload.put("kycExpiresAt", java.time.Instant.now().plusSeconds(86400).toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.KYC_APPROVED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.KYC_APPROVED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.KYC_APPROVED, event));
    }

    private void ingestDividendDistributed(UUID eventId) throws Exception {
        Map<String, Object> payout = new LinkedHashMap<>();
        payout.put("dividendPaymentId", UUID.randomUUID().toString());
        payout.put("investorId", UUID.randomUUID().toString());
        payout.put("walletAddress", "0xHolder");
        payout.put("amount", "150.00");
        payout.put("ownershipPct", "70.00");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("period", "2025-09");
        payload.put("totalAmountUsd", "1500.00");
        payload.put("holderPayouts", java.util.List.of(payout));
        payload.put("distributedAt", java.time.Instant.now().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.DIVIDEND_DISTRIBUTED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.DIVIDEND_DISTRIBUTED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.DIVIDEND_DISTRIBUTED, event));
    }

    private void ingestFlatTokenized(UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("buildingId", UUID.randomUUID().toString());
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("contractAddress", "0xDemoPropertyToken");
        payload.put("totalSupply", 1000);
        payload.put("tokenPriceUsd", "45.00");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.FLAT_TOKENIZED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.FLAT_TOKENIZED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.FLAT_TOKENIZED, event));
    }

    private void ingestOrderMatched(UUID buyerId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", UUID.randomUUID().toString());
        payload.put("tradeId", UUID.randomUUID().toString());
        payload.put("listingId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("buyerId", buyerId.toString());
        payload.put("sellerId", UUID.randomUUID().toString());
        payload.put("tokenAmount", 10);
        payload.put("totalPriceUsd", "450.00");
        payload.put("paymentId", UUID.randomUUID().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.ORDER_MATCHED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.ORDER_MATCHED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.ORDER_MATCHED, event));
    }

    private void ingestListingCreated(UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listingId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("listingType", "PRIMARY");
        payload.put("priceUsd", "45.00");
        payload.put("tokensAvailable", 1000);

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.LISTING_CREATED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.LISTING_CREATED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.LISTING_CREATED, event));
    }

    private void ingestPaymentConfirmed(UUID payerId, UUID eventId) throws Exception {
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("value", "4500.00");
        amount.put("currency", "USDC");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", UUID.randomUUID().toString());
        payload.put("orderId", UUID.randomUUID().toString());
        payload.put("payerId", payerId.toString());
        payload.put("amount", amount);
        payload.put("txHash", "0xPaymentConfirmed");
        payload.put("confirmedAt", java.time.Instant.now().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.PAYMENT_CONFIRMED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.PAYMENT_CONFIRMED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.PAYMENT_CONFIRMED, event));
    }

    private void ingestTransferCompleted(UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferId", UUID.randomUUID().toString());
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("orderId", UUID.randomUUID().toString());
        payload.put("tradeId", UUID.randomUUID().toString());
        payload.put("paymentId", UUID.randomUUID().toString());
        payload.put("fromWallet", "0xFrom");
        payload.put("toWallet", "0xTo");
        payload.put("tokenAmount", 100);
        payload.put("txHash", "0xTransferCompleted");
        payload.put("completedAt", java.time.Instant.now().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.TRANSFER_COMPLETED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.TRANSFER_COMPLETED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.TRANSFER_COMPLETED, event));
    }

    private void ingestTradeSettled(UUID buyerId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buyerId", buyerId.toString());
        payload.put("orderId", UUID.randomUUID().toString());
        payload.put("tradeId", UUID.randomUUID().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                NotificationKafkaEventTypes.TRADE_SETTLED,
                "test-trace",
                payload));
        eventConsumer.consume(message, NotificationKafkaEventTypes.TRADE_SETTLED, "Notification ingest failed",
                event -> notificationLogService.logEvent(NotificationKafkaEventTypes.TRADE_SETTLED, event));
    }
}
