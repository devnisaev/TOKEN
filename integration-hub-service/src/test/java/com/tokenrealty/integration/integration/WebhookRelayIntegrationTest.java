package com.tokenrealty.integration.integration;

import com.tokenrealty.integration.client.ComplianceClient;
import com.tokenrealty.integration.entity.IntegrationDelivery;
import com.tokenrealty.integration.entity.IntegrationDeliveryStatus;
import com.tokenrealty.integration.entity.IntegrationType;
import com.tokenrealty.integration.repository.IntegrationDeliveryRepository;
import com.tokenrealty.integration.service.IntegrationDeliveryRetryWorker;
import com.tokenrealty.web.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Webhook relay integration test")
class WebhookRelayIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired IntegrationDeliveryRepository deliveryRepository;
    @Autowired IntegrationDeliveryRetryWorker retryWorker;

    @MockitoBean ComplianceClient complianceClient;

    @BeforeEach
    void cleanDeliveries() {
        deliveryRepository.deleteAll();
    }

    @Test
    void kycWebhook_relaysToComplianceAndMarksDelivered() throws Exception {
        String body = "{\"externalId\":\"applicant-123\",\"status\":\"completed\"}";

        mockMvc.perform(post("/v1/integrations/webhooks/kyc/sumsub")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payload-Digest", "abc123")
                        .header("X-Signature", "sig456")
                        .content(body))
                .andExpect(status().isNoContent());

        verify(complianceClient).forwardKycWebhook("sumsub", body, "abc123", "sig456");

        IntegrationDelivery delivery = deliveryRepository.findAll().getFirst();
        assertThat(delivery.getIntegrationType()).isEqualTo(IntegrationType.KYC);
        assertThat(delivery.getProvider()).isEqualTo("sumsub");
        assertThat(delivery.getPayload()).isEqualTo(body);
        assertThat(delivery.getPayloadDigest()).isEqualTo("abc123");
        assertThat(delivery.getSignature()).isEqualTo("sig456");
        assertThat(delivery.getStatus()).isEqualTo(IntegrationDeliveryStatus.DELIVERED);
        assertThat(delivery.getAttempts()).isZero();
    }

    @Test
    void kycWebhook_marksFailedWhenComplianceUnavailable() throws Exception {
        String body = "{\"externalId\":\"applicant-456\"}";
        doThrow(new ValidationException("Compliance service unavailable"))
                .when(complianceClient)
                .forwardKycWebhook(eq("onfido"), eq(body), eq(null), eq(null));

        mockMvc.perform(post("/v1/integrations/webhooks/kyc/onfido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        IntegrationDelivery delivery = deliveryRepository.findAll().getFirst();
        assertThat(delivery.getStatus()).isEqualTo(IntegrationDeliveryStatus.FAILED);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getNextRetryAt()).isAfter(Instant.now());
        assertThat(delivery.getLastError()).contains("Compliance service unavailable");
    }

    @Test
    void retryWorker_deliversPreviouslyFailedWebhook() {
        IntegrationDelivery delivery = deliveryRepository.save(IntegrationDelivery.builder()
                .integrationType(IntegrationType.KYC)
                .provider("sumsub")
                .payload("{\"externalId\":\"retry-me\"}")
                .status(IntegrationDeliveryStatus.FAILED)
                .attempts(1)
                .nextRetryAt(Instant.now().minusSeconds(60))
                .lastError("Compliance service unavailable")
                .build());

        retryWorker.retryPendingDeliveries();

        IntegrationDelivery updated = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(IntegrationDeliveryStatus.DELIVERED);
        verify(complianceClient).forwardKycWebhook("sumsub", "{\"externalId\":\"retry-me\"}", null, null);
    }
}
