package com.tokenrealty.compliance.client;

import com.tokenrealty.compliance.config.OnfidoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/** Outbound Onfido applicant creation (track 240). When disabled, returns a local reference id. */
@Component
@ConditionalOnProperty(name = "tokenrealty.compliance.onfido.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OnfidoClient {

    private final RestClient.Builder restClientBuilder;
    private final OnfidoProperties properties;

    public String createApplicant(UUID investorId, String fullName, String countryCode) {
        if (properties.getApiToken() == null || properties.getApiToken().isBlank()) {
            return "onfido-local-" + investorId;
        }
        try {
            RestClient client = restClientBuilder.baseUrl(properties.getApiUrl()).build();
            var response = client.post()
                    .uri("/applicants")
                    .header("Authorization", "Token token=" + properties.getApiToken())
                    .body(Map.of(
                            "first_name", firstName(fullName),
                            "last_name", lastName(fullName),
                            "location", Map.of("country_of_residence", countryCode != null ? countryCode : "US")))
                    .retrieve()
                    .body(Map.class);
            Object id = response != null ? response.get("id") : null;
            String applicantId = id != null ? id.toString() : "onfido-" + investorId;
            log.info("Created Onfido applicant {} for investor {}", applicantId, investorId);
            return applicantId;
        } catch (Exception ex) {
            log.warn("Onfido applicant create failed for {}: {}", investorId, ex.getMessage());
            return "onfido-fallback-" + investorId;
        }
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Investor";
        }
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(0, space) : fullName;
    }

    private static String lastName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "User";
        }
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(space + 1) : "User";
    }
}
