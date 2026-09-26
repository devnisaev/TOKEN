package com.tokenrealty.compliance.client;

import com.tokenrealty.compliance.config.SumsubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Outbound Sumsub applicant creation (track 194). When disabled, returns a local reference id.
 */
@Component
@ConditionalOnProperty(name = "tokenrealty.compliance.sumsub.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SumsubClient {

    private final RestClient.Builder restClientBuilder;
    private final SumsubProperties properties;

    public String createApplicant(UUID investorId, String fullName, String countryCode) {
        if (properties.getAppToken() == null || properties.getAppToken().isBlank()) {
            return "sumsub-local-" + investorId;
        }
        try {
            RestClient client = restClientBuilder.baseUrl(properties.getApiUrl()).build();
            var response = client.post()
                    .uri("/resources/applicants?levelName={level}", properties.getLevelName())
                    .header("X-App-Token", properties.getAppToken())
                    .header("X-App-Access-Sig", properties.getAppSecret())
                    .body(Map.of(
                            "externalUserId", investorId.toString(),
                            "info", Map.of("firstName", fullName, "country", countryCode)))
                    .retrieve()
                    .body(Map.class);
            Object id = response != null ? response.get("id") : null;
            String applicantId = id != null ? id.toString() : "sumsub-" + investorId;
            log.info("Created Sumsub applicant {} for investor {}", applicantId, investorId);
            return applicantId;
        } catch (Exception ex) {
            log.warn("Sumsub applicant create failed for {}: {}", investorId, ex.getMessage());
            return "sumsub-fallback-" + investorId;
        }
    }
}
