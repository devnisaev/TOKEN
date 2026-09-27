package com.tokenrealty.valuation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.valuation.dto.ValuationDtos.SubmitValuationRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.ValuationRequestResponse;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValuationFeedService {

    private static final UUID SYSTEM_FEED_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ValuationService valuationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ValuationRequestResponse ingestFeed(String provider, String rawBody) {
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            UUID buildingId = uuid(payload, "buildingId");
            UUID flatId = uuid(payload, "flatId");
            BigDecimal valueUsd = decimal(payload, "valueUsd");
            long totalTokens = payload.path("totalTokens").asLong(0);
            if (totalTokens <= 0) {
                raiseValidation("totalTokens must be positive");
            }
            String notes = "AVM feed from " + provider;
            if (payload.hasNonNull("notes")) {
                notes = payload.get("notes").asText();
            }
            BigDecimal operatingUtilizationPct = payload.hasNonNull("operatingUtilizationPct")
                    ? new BigDecimal(payload.get("operatingUtilizationPct").asText()) : null;
            Integer memberCountKpi = payload.hasNonNull("memberCountKpi")
                    ? payload.get("memberCountKpi").asInt() : null;
            return valuationService.submit(new SubmitValuationRequest(
                    buildingId, flatId, valueUsd, totalTokens, notes,
                    operatingUtilizationPct, memberCountKpi), SYSTEM_FEED_ACTOR);
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Invalid valuation feed payload: " + ex.getMessage());
        }
    }

    private static UUID uuid(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            raiseValidation("Missing field: " + field);
        }
        return UUID.fromString(node.get(field).asText());
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            raiseValidation("Missing field: " + field);
        }
        return new BigDecimal(node.get(field).asText());
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
