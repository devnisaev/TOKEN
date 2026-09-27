package com.tokenrealty.rental.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.rental.dto.RentalDtos.IngestOperatorRevenueRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Operator revenue ingestion — Phase 12")
class OperatorRevenueIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;
    UUID flatId = UUID.randomUUID();
    UUID buildingId = UUID.randomUUID();

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Ingest operator revenue and list by flat")
    void ingestAndList() throws Exception {
        var req = new IngestOperatorRevenueRequest(
                buildingId,
                flatId,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                new BigDecimal("12500.00"),
                "FitZone Operator LLC");

        mockMvc.perform(post("/v1/operator-revenue/ingest")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grossRevenueUsd").value(12500.00))
                .andExpect(jsonPath("$.sourceProvider").value("direct"));

        mockMvc.perform(get("/v1/operator-revenue").param("flatId", flatId.toString()).with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flatId").value(flatId.toString()));
    }
}
