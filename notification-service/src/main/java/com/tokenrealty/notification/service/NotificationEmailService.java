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
            case "tokenrealty.registry.flat.tokenized.v1" -> "Property tokenized";
            case "tokenrealty.marketplace.listing.created.v1" -> "New listing available";
            case "tokenrealty.marketplace.order.matched.v1" -> "Order matched";
            case "tokenrealty.payment.payment.confirmed.v1" -> "Payment confirmed";
            case "tokenrealty.issuance.transfer.completed.v1" -> "Token transfer completed";
            case "tokenrealty.document.document.uploaded.v1" -> "New data room document uploaded";
            default -> "TokenRealty notification: " + eventType;
        };
    }

    private String buildBody(String eventType, JsonNode payload) {
        return "Event: " + eventType + "\n\nDetails:\n" + payload;
    }

    private static String textOrNull(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node != null && !node.isNull() ? node.asText() : null;
    }
}
