package com.tokenrealty.registry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.registry.config.JacksonConfig;
import com.tokenrealty.registry.config.SecurityConfig;
import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.exception.ConflictException;
import com.tokenrealty.registry.exception.GlobalExceptionHandler;
import com.tokenrealty.registry.exception.ResourceNotFoundException;
import com.tokenrealty.registry.service.BuildingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuildingController.class)
@Import({GlobalExceptionHandler.class, JacksonConfig.class, SecurityConfig.class})
@DisplayName("BuildingController MockMvc tests")
class BuildingControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean BuildingService buildingService;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private static final UUID BUILDING_ID = UUID.randomUUID();

    private BuildingResponse sampleResponse() {
        return new BuildingResponse(
                BUILDING_ID, "Sunrise Tower", "123 Main St", "Bishkek", "KG",
                null, null, null, null, null,
                Building.BuildingStatus.PENDING_REVIEW,
                null, null, 0, Instant.now(), Instant.now());
    }

    // ─── GET /v1/buildings ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/buildings returns 200")
    void listAll_returns200() throws Exception {
        when(buildingService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/v1/buildings").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Sunrise Tower"));
    }

    @Test
    @DisplayName("GET /v1/buildings?search=Sunrise delegates to search()")
    void listAll_withSearch_callsSearch() throws Exception {
        when(buildingService.search(eq("Sunrise"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/v1/buildings").with(user("user")).param("search", "Sunrise"))
                .andExpect(status().isOk());

        verify(buildingService).search(eq("Sunrise"), any());
    }

    // ─── GET /v1/buildings/{id} ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/buildings/{id} returns 200")
    void getById_returns200() throws Exception {
        var detail = BuildingDetailResponse.builder()
                .id(BUILDING_ID).name("Sunrise Tower").city("Bishkek")
                .status(Building.BuildingStatus.APPROVED)
                .flatCount(0).flats(List.of()).documents(List.of())
                .build();

        when(buildingService.findById(BUILDING_ID)).thenReturn(detail);

        mockMvc.perform(get("/v1/buildings/{id}", BUILDING_ID).with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /v1/buildings/{id} returns 404 when not found")
    void getById_returns404_whenNotFound() throws Exception {
        when(buildingService.findById(BUILDING_ID))
                .thenThrow(new ResourceNotFoundException("Building", BUILDING_ID));

        mockMvc.perform(get("/v1/buildings/{id}", BUILDING_ID).with(user("user")))
                .andExpect(status().isNotFound());
    }

    // ─── POST /v1/buildings ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /v1/buildings returns 201 when valid")
    void create_returns201() throws Exception {
        var request = new CreateBuildingRequest(
                "Sunrise Tower", "123 Main St", "Bishkek", "KG",
                "720001", 10, 40, 2020, 3500.0, 42.87, 74.59);

        when(buildingService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/v1/buildings")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sunrise Tower"));
    }

    @Test
    @DisplayName("POST /v1/buildings returns 400 when name is blank")
    void create_returns400_whenNameBlank() throws Exception {
        var request = new CreateBuildingRequest(
                "", "123 Main St", "Bishkek", "KG",
                null, null, null, null, null, null, null);

        mockMvc.perform(post("/v1/buildings")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @DisplayName("POST /v1/buildings returns 409 when duplicate")
    void create_returns409_whenDuplicate() throws Exception {
        var request = new CreateBuildingRequest(
                "Duplicate", "123 Main St", "Bishkek", "KG",
                null, null, null, null, null, null, null);

        when(buildingService.create(any()))
                .thenThrow(new ConflictException("Building already registered at this address in Bishkek"));

        mockMvc.perform(post("/v1/buildings")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /v1/buildings returns 403 for INVESTOR role")
    void create_returns403_forInvestorRole() throws Exception {
        var request = new CreateBuildingRequest(
                "Sunrise Tower", "123 Main St", "Bishkek", "KG",
                null, 5, 20, 2020, 1000.0, null, null);

        mockMvc.perform(post("/v1/buildings")
                        .with(user("investor").roles("INVESTOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ─── PATCH /v1/buildings/{id}/status ────────────────────────────────────

    @Test
    @DisplayName("PATCH /v1/buildings/{id}/status returns 200")
    void updateStatus_returns200() throws Exception {
        when(buildingService.updateStatus(BUILDING_ID, Building.BuildingStatus.APPROVED))
                .thenReturn(new BuildingResponse(
                        BUILDING_ID, "Sunrise Tower", "123 Main St", "Bishkek", "KG",
                        null, null, null, null, null,
                        Building.BuildingStatus.APPROVED,
                        null, null, 0, Instant.now(), Instant.now()));

        mockMvc.perform(patch("/v1/buildings/{id}/status", BUILDING_ID)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // ─── DELETE /v1/buildings/{id} ───────────────────────────────────────────

    @Test
    @DisplayName("DELETE /v1/buildings/{id} returns 204")
    void delete_returns204() throws Exception {
        doNothing().when(buildingService).delete(BUILDING_ID);

        mockMvc.perform(delete("/v1/buildings/{id}", BUILDING_ID)
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /v1/buildings/{id} returns 409 when tokenized")
    void delete_returns409_whenTokenized() throws Exception {
        doThrow(new ConflictException("Cannot delete a building with active token contracts"))
                .when(buildingService).delete(BUILDING_ID);

        mockMvc.perform(delete("/v1/buildings/{id}", BUILDING_ID)
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict());
    }
}