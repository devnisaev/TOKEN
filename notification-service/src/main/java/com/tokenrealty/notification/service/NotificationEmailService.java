package com.tokenrealty.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class NotificationEmailService {

    private final Optional<JavaMailSender> mailSender;

    @Value("${tokenrealty.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${tokenrealty.notification.email.mode:log}")
    private String emailMode;

    @Value("${tokenrealty.notification.email.default-recipient:admin@tokenrealty.com}")
    private String defaultRecipient;

    @Value("${tokenrealty.notification.email.from:noreply@tokenrealty.com}")
    private String fromAddress;

    public NotificationEmailService(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String eventType, JsonNode payload) {
        String recipient = resolveRecipient(payload);
        String subject = buildSubject(eventType);
        String body = buildBody(eventType, payload);

        if (!emailEnabled) {
            log.debug("[EMAIL disabled] type={} recipient={} subject={}", eventType, recipient, subject);
            return;
        }

        if ("smtp".equalsIgnoreCase(emailMode) && mailSender.isPresent()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.get().send(message);
            log.info("[EMAIL sent] type={} recipient={}", eventType, recipient);
            return;
        }

        log.info("[EMAIL log-mode] type={} recipient={} subject={} body={}",
                eventType, recipient, subject, body);
    }

    private String resolveRecipient(JsonNode payload) {
        String email = textOrNull(payload, "email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        String investorId = textOrNull(payload, "investorId");
        if (investorId != null) {
            return investorId + "@investor.tokenrealty.dev";
        }
        String buyerId = textOrNull(payload, "buyerId");
        if (buyerId != null) {
            return buyerId + "@investor.tokenrealty.dev";
        }
        return defaultRecipient;
    }

    private String buildSubject(String eventType) {
        return switch (eventType) {
            case "tokenrealty.compliance.investor.kyc-approved.v1" -> "Your KYC has been approved";
            case "tokenrealty.compliance.investor.kyc-revoked.v1" -> "Your KYC status has changed";
            case "tokenrealty.marketplace.trade.settled.v1" -> "Your trade has settled";
            case "tokenrealty.issuance.dividend.distributed.v1" -> "Dividend payment distributed";
            case "tokenrealty.payment.rent.collected.v1" -> "Rent payment collected";
            case "tokenrealty.registry.building.approved.v1" -> "Building approved";
            case "tokenrealty.registry.flat.tokenized.v1" -> "Property tokenized";
            case "tokenrealty.rental.rent.due.v1" -> "Rent payment due";
            case "tokenrealty.rental.lease.expired.v1" -> "Lease expired";
            case "tokenrealty.marketplace.listing.created.v1" -> "New listing available";
            case "tokenrealty.marketplace.order.matched.v1" -> "Order matched";
            case "tokenrealty.payment.payment.confirmed.v1" -> "Payment confirmed";
            case "tokenrealty.issuance.transfer.completed.v1" -> "Token transfer completed";
            case "tokenrealty.document.document.uploaded.v1" -> "New data room document uploaded";
            case "tokenrealty.settlement.stuck.v1" -> "Settlement saga stuck — action required";
            case "tokenrealty.settlement.recovered.v1" -> "Settlement saga recovered";
            case "tokenrealty.valuation.approved.v1" -> "Property valuation approved";
            default -> "TokenRealty notification: " + eventType;
        };
    }

    private String buildBody(String eventType, JsonNode payload) {
        return switch (eventType) {
            case "tokenrealty.compliance.investor.kyc-approved.v1" -> """
                    Hello,

                    Your KYC verification has been approved. You may now invest on TokenRealty.

                    Wallet: %s
                    Approved at: %s

                    — TokenRealty
                    """.formatted(
                    textOr(payload, "walletAddress", "n/a"),
                    textOr(payload, "approvedAt", "n/a"));
            case "tokenrealty.marketplace.trade.settled.v1" -> """
                    Hello,

                    Your trade has settled successfully.

                    Trade ID: %s
                    Order ID: %s
                    Listing ID: %s
                    Payment ID: %s

                    — TokenRealty
                    """.formatted(
                    textOr(payload, "tradeId", "n/a"),
                    textOr(payload, "orderId", "n/a"),
                    textOr(payload, "listingId", "n/a"),
                    textOr(payload, "paymentId", "n/a"));
            case "tokenrealty.issuance.dividend.distributed.v1" -> """
                    Hello,

                    A dividend has been distributed for your property holding.

                    Flat ID: %s
                    Period: %s
                    Total amount: %s %s
                    Distributed at: %s

                    — TokenRealty
                    """.formatted(
                    textOr(payload, "flatId", "n/a"),
                    textOr(payload, "period", "n/a"),
                    amountValue(payload, "totalAmount"),
                    amountCurrency(payload, "totalAmount"),
                    textOr(payload, "distributedAt", "n/a"));
            case "tokenrealty.rental.rent.due.v1" -> """
                    Hello,

                    Your rent payment is due.

                    Lease ID: %s
                    Period: %s
                    Amount due: $%s
                    Due date: %s

                    Please pay through the tenant portal.

                    — TokenRealty
                    """.formatted(
                    textOr(payload, "leaseId", "n/a"),
                    textOr(payload, "period", "n/a"),
                    textOr(payload, "amountUsd", "n/a"),
                    textOr(payload, "dueDate", "n/a"));
            case "tokenrealty.marketplace.order.matched.v1" -> """
                    Hello,

                    Your buy order has been matched.

                    Order ID: %s
                    Listing ID: %s
                    Tokens: %s
                    Total price: $%s

                    Complete payment to proceed with settlement.

                    — TokenRealty
                    """.formatted(
                    textOr(payload, "orderId", "n/a"),
                    textOr(payload, "listingId", "n/a"),
                    textOr(payload, "tokenAmount", "n/a"),
                    textOr(payload, "totalPriceUsd", "n/a"));
            case "tokenrealty.settlement.stuck.v1" -> """
                    Hello,

                    A settlement saga has exceeded its SLA and requires attention.

                    Saga ID: %s
                    Order ID: %s
                    Current step: %s
                    Stuck at: %s

                    — TokenRealty Ops
                    """.formatted(
                    textOr(payload, "sagaId", "n/a"),
                    textOr(payload, "orderId", "n/a"),
                    textOr(payload, "currentStep", "n/a"),
                    textOr(payload, "stuckAt", "n/a"));
            case "tokenrealty.settlement.recovered.v1" -> """
                    Hello,

                    A previously stuck settlement saga has recovered.

                    Saga ID: %s
                    Order ID: %s
                    Current step: %s
                    Recovered at: %s

                    — TokenRealty Ops
                    """.formatted(
                    textOr(payload, "sagaId", "n/a"),
                    textOr(payload, "orderId", "n/a"),
                    textOr(payload, "currentStep", "n/a"),
                    textOr(payload, "recoveredAt", "n/a"));
            case "tokenrealty.valuation.approved.v1" -> """
                    Hello,

                    A property valuation has been approved.

                    Flat ID: %s
                    Building ID: %s
                    Value (USD): %s
                    NAV per token: %s
                    Approved at: %s

                    — TokenRealty
                    """.formatted(
                    textOr(payload, "flatId", "n/a"),
                    textOr(payload, "buildingId", "n/a"),
                    textOr(payload, "valueUsd", "n/a"),
                    textOr(payload, "navPerTokenUsd", "n/a"),
                    textOr(payload, "approvedAt", "n/a"));
            default -> "Event: " + eventType + "\n\nDetails:\n" + payload;
        };
    }

    private static String textOrNull(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private static String textOr(JsonNode payload, String field, String fallback) {
        String value = textOrNull(payload, field);
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static String amountValue(JsonNode payload, String field) {
        JsonNode amount = payload.get(field);
        if (amount == null || amount.isNull()) {
            return "n/a";
        }
        JsonNode value = amount.get("value");
        return value != null && !value.isNull() ? value.asText() : amount.asText();
    }

    private static String amountCurrency(JsonNode payload, String field) {
        JsonNode amount = payload.get(field);
        if (amount == null || amount.isNull()) {
            return "USD";
        }
        JsonNode currency = amount.get("currency");
        return currency != null && !currency.isNull() ? currency.asText() : "USD";
    }
}
