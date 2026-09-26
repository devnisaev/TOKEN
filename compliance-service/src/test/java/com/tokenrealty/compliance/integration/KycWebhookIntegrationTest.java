package com.tokenrealty.compliance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.compliance.dto.ComplianceDtos.KycReviewResult;
import com.tokenrealty.compliance.dto.ComplianceDtos.KycWebhookPayload;
import com.tokenrealty.compliance.dto.ComplianceDtos.RegisterComplianceRequest;
import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import com.tokenrealty.compliance.service.ComplianceService;
import com.tokenrealty.compliance.service.KycWebhookService;
import com.tokenrealty.compliance.service.KycWebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("KYC webhook integration test")
class KycWebhookIntegrationTest {

    @Autowired ComplianceService complianceService;
    @Autowired KycWebhookService kycWebhookService;
    @Autowired ComplianceRecordRepository complianceRecordRepository;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired KycWebhookSignatureVerifier signatureVerifier;

    UUID investorId;

    @BeforeEach
    void setUp() {
        investorId = UUID.randomUUID();
        complianceService.register(new RegisterComplianceRequest(
                investorId,
                "0xwebhook" + investorId.toString().replace("-", "").substring(0, 24),
                "Webhook Investor",
                "KG",
                "sumsub",
                "applicant-" + investorId.toString().substring(0, 8)));
    }

    @Test
    void sumsubGreenWebhook_approvesInvestor() {
        KycWebhookPayload payload = new KycWebhookPayload(
                "applicant-" + investorId.toString().substring(0, 8),
                investorId.toString(),
                "completed",
                Instant.parse("2027-01-01T00:00:00Z"),
                new KycReviewResult("GREEN", List.of()));

        kycWebhookService.handleWebhook("sumsub", payload);

        ComplianceRecord record = complianceRecordRepository.findByInvestorId(investorId).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(ComplianceRecord.ComplianceStatus.APPROVED);
        assertThat(record.getKycVerifiedAt()).isNotNull();
    }

    @Test
    void sumsubRedWebhook_revokesInvestor() {
        complianceService.verify(
                complianceRecordRepository.findByInvestorId(investorId).orElseThrow().getId(),
                new com.tokenrealty.compliance.dto.ComplianceDtos.ComplianceVerifyRequest(
                        Instant.parse("2027-01-01T00:00:00Z")));

        KycWebhookPayload payload = new KycWebhookPayload(
                "applicant-" + investorId.toString().substring(0, 8),
                investorId.toString(),
                "completed",
                null,
                new KycReviewResult("RED", List.of("DOCUMENT_MISMATCH")));

        kycWebhookService.handleWebhook("sumsub", payload);

        ComplianceRecord record = complianceRecordRepository.findByInvestorId(investorId).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(ComplianceRecord.ComplianceStatus.REVOKED);
        assertThat(record.getRejectionReason()).contains("DOCUMENT_MISMATCH");
    }

    @Test
    void sumsubWebhook_rejectsInvalidSignatureWhenSecretConfigured() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(
                signatureVerifier, "webhookSecret", "test-webhook-secret");
        KycWebhookPayload payload = new KycWebhookPayload(
                "applicant-" + investorId.toString().substring(0, 8),
                investorId.toString(),
                "completed",
                Instant.parse("2027-01-01T00:00:00Z"),
                new KycReviewResult("GREEN", List.of()));
        String rawBody = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/v1/compliance/webhooks/kyc/sumsub")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payload-Digest", "deadbeef")
                        .content(rawBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void sumsubWebhook_acceptsValidHmacSignature() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(
                signatureVerifier, "webhookSecret", "test-webhook-secret");
        KycWebhookPayload payload = new KycWebhookPayload(
                "applicant-" + investorId.toString().substring(0, 8),
                investorId.toString(),
                "completed",
                Instant.parse("2027-01-01T00:00:00Z"),
                new KycReviewResult("GREEN", List.of()));
        String rawBody = objectMapper.writeValueAsString(payload);
        String digest = signatureVerifier.computeHmacSha256Hex(rawBody);

        mockMvc.perform(post("/v1/compliance/webhooks/kyc/sumsub")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payload-Digest", digest)
                        .content(rawBody))
                .andExpect(status().isNoContent());

        ComplianceRecord record = complianceRecordRepository.findByInvestorId(investorId).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(ComplianceRecord.ComplianceStatus.APPROVED);
    }
}
