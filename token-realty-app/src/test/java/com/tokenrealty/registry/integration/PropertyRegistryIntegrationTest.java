package com.tokenrealty.registry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.PropertyDocument;
import com.tokenrealty.registry.entity.SpvEntity;
import com.tokenrealty.registry.entity.Valuation;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Property Registry — full integration tests (H2)")
class PropertyRegistryIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;

    static String buildingId;
    static String flatId;
    static String spvId;
    static String valuationId;
    static String docId;

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test @Order(1)
    @DisplayName("1. Create building with metadata → 201")
    void step1_createBuilding() throws Exception {
        var req = new CreateBuildingRequest(
                "Sunrise Tower", "1 Chui Ave", "Bishkek", "KG",
                "720001", 12, 48, 2022, 4800.0, 42.8746, 74.5698,
                Building.PropertyCategory.RESIDENTIAL_FLAT, "KG-CAD-001",
                "A+", "C-2", 2020);

        MvcResult result = mockMvc.perform(post("/v1/buildings")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sunrise Tower"))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.energyEfficiencyRating").value("A+"))
                .andExpect(jsonPath("$.zoningCode").value("C-2"))
                .andReturn();

        buildingId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
        assertThat(buildingId).isNotBlank();
    }

    @Test @Order(2)
    @DisplayName("2. Duplicate building address → 409")
    void step2_duplicateBuildingConflict() throws Exception {
        var req = new CreateBuildingRequest(
                "Another Tower", "1 Chui Ave", "Bishkek", "KG",
                null, 5, 20, 2020, 1000.0, null, null,
                null, null, null, null, null);

        mockMvc.perform(post("/v1/buildings")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @Order(3)
    @DisplayName("3. GET building by id → 200")
    void step3_getBuildingById() throws Exception {
        mockMvc.perform(get("/v1/buildings/{id}", buildingId).with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sunrise Tower"))
                .andExpect(jsonPath("$.lastRenovationYear").value(2020));
    }

    @Test @Order(4)
    @DisplayName("4. Register SPV → 201, building becomes APPROVED")
    void step4_registerSpv() throws Exception {
        var req = new CreateSpvRequest(
                "Sunrise Realty SPV LLC", "KG-2024-001", "KG",
                LocalDate.of(2024, 1, 10), "1 Chui Ave, Bishkek", null, "TAX-KG-001",
                null, SpvEntity.OwnershipType.SPV_SHARE_EQUITY);

        MvcResult result = mockMvc.perform(post("/v1/buildings/{id}/spv", buildingId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legalName").value("Sunrise Realty SPV LLC"))
                .andReturn();

        spvId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/v1/buildings/{id}", buildingId).with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test @Order(5)
    @DisplayName("5. Verify SPV KYC → status ACTIVE")
    void step5_verifySpvKyc() throws Exception {
        mockMvc.perform(patch("/v1/buildings/{bid}/spv/{sid}/kyc", buildingId, spvId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .param("verified", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycVerified").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @Order(6)
    @DisplayName("6. Add flat → 201")
    void step6_addFlat() throws Exception {
        var req = new CreateFlatRequest("1A", 1, 72.5, 3, 1, null, null);

        MvcResult result = mockMvc.perform(post("/v1/buildings/{id}/flats", buildingId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flatNumber").value("1A"))
                .andReturn();

        flatId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test @Order(7)
    @DisplayName("7. Duplicate flat number → 409")
    void step7_duplicateFlatConflict() throws Exception {
        var req = new CreateFlatRequest("1A", 1, 60.0, 2, 1, null, null);
        mockMvc.perform(post("/v1/buildings/{id}/flats", buildingId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @Order(8)
    @DisplayName("8. Add valuation → 201")
    void step8_addValuation() throws Exception {
        var req = new CreateValuationRequest(
                LocalDate.of(2024, 4, 1),
                BigDecimal.valueOf(85_000),
                BigDecimal.valueOf(7_565_000), "KGS",
                "Aibek D.", "KG-APP-0042",
                Valuation.ValuationMethod.COMPARABLE_SALES,
                BigDecimal.valueOf(6_000), null, null, "Market analysis");

        MvcResult result = mockMvc.perform(post("/v1/flats/{id}/valuations", flatId)
                        .with(user("appraiser").roles("APPRAISER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isCurrent").value(true))
                .andReturn();

        valuationId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test @Order(9)
    @DisplayName("9. GET current valuation → 200")
    void step9_getCurrentValuation() throws Exception {
        mockMvc.perform(get("/v1/flats/{id}/valuations/current", flatId).with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCurrent").value(true));
    }

    @Test @Order(10)
    @DisplayName("10. Register title deed → 201")
    void step10_registerDocument() throws Exception {
        var req = new RegisterDocumentRequest(
                "Title Deed 2024", PropertyDocument.DocumentType.TITLE_DEED,
                "QmTitleDeedCid123456789abcdef", null, 204800L, "application/pdf");

        MvcResult result = mockMvc.perform(post("/v1/buildings/{id}/documents", buildingId)
                        .with(user("manager").roles("PROPERTY_MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isVerified").value(false))
                .andReturn();

        docId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test @Order(11)
    @DisplayName("11. Verify document → isVerified=true")
    void step11_verifyDocument() throws Exception {
        mockMvc.perform(patch("/v1/documents/{id}/verify", docId).with(user("compliance").roles("COMPLIANCE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true));
    }

    @Test @Order(12)
    @DisplayName("12. Delete verified document → 409")
    void step12_deleteVerifiedDocumentFails() throws Exception {
        mockMvc.perform(delete("/v1/documents/{id}", docId).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @Order(13)
    @DisplayName("13. Set token info → TOKENIZED")
    void step13_setTokenInfo() throws Exception {
        mockMvc.perform(patch("/v1/flats/{id}/token-info", flatId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .param("contractAddress", "0xFakeContractAddress1234")
                        .param("totalTokens", "1000")
                        .param("tokenPriceUsd", "85.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOKENIZED"));
    }

    @Test @Order(14)
    @DisplayName("14. Delete TOKENIZED flat → 409")
    void step14_deleteTokenizedFlatFails() throws Exception {
        mockMvc.perform(delete("/v1/flats/{id}", flatId).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @Order(15)
    @DisplayName("15. Delete TOKENIZED building → 409")
    void step15_deleteTokenizedBuildingFails() throws Exception {
        mockMvc.perform(delete("/v1/buildings/{id}", buildingId).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @Order(16)
    @DisplayName("16. INVESTOR can read flat → 200")
    void step16_investorCanReadFlat() throws Exception {
        mockMvc.perform(get("/v1/flats/{id}", flatId).with(user("investor").roles("INVESTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flatNumber").value("1A"));
    }

    @Test @Order(17)
    @DisplayName("17. INVESTOR cannot create building → 403")
    void step17_investorCannotCreateBuilding() throws Exception {
        var req = new CreateBuildingRequest(
                "Investor Tower", "99 Side St", "Bishkek", "KG",
                null, 3, 10, 2023, 500.0, null, null,
                null, null, null, null, null);

        mockMvc.perform(post("/v1/buildings")
                        .with(user("investor").roles("INVESTOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
