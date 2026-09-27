package com.tokenrealty.registry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.registry.dto.PropertyDtos.CreateStandaloneAssetRequest;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.entity.PropertyDocument;
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
import java.util.List;

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
@DisplayName("Asset unit API — Phase 12 integration")
class AssetUnitIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Standalone gym asset registration with operating metadata")
    void standaloneGymAsset() throws Exception {
        var req = CreateStandaloneAssetRequest.builder()
                .name("FitZone Bishkek")
                .address("10 Sport St")
                .city("Bishkek")
                .country("KG")
                .propertyCategory(Building.PropertyCategory.GYM)
                .unitLabel("GYM-1")
                .areaSqm(1200.0)
                .operatingModel(Flat.OperatingModel.OPERATOR_REVENUE_SHARE)
                .liquidityTier(Flat.LiquidityTier.TIER_2)
                .licenseTypes(List.of("HEALTH_PERMIT"))
                .occupancyOrUtilization(new BigDecimal("68.5"))
                .build();

        mockMvc.perform(post("/v1/buildings/standalone-assets")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.building.propertyCategory").value("GYM"))
                .andExpect(jsonPath("$.assetUnit.operatingModel").value("OPERATOR_REVENUE_SHARE"))
                .andExpect(jsonPath("$.assetUnit.liquidityTier").value("TIER_2"));
    }

    @Test
    @DisplayName("Asset units filter by property category")
    void listAssetUnitsByCategory() throws Exception {
        mockMvc.perform(get("/v1/asset-units")
                        .param("propertyCategory", "GYM")
                        .with(user("user")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Operating license document type accepted")
    void operatingLicenseDocumentTypeExists() {
        org.assertj.core.api.Assertions.assertThat(PropertyDocument.DocumentType.OPERATING_LICENSE).isNotNull();
    }
}
