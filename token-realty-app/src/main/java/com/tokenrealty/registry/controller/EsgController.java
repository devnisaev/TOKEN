package com.tokenrealty.registry.controller;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.service.EsgService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/esg")
@RequiredArgsConstructor
public class EsgController {

    private final EsgService esgService;

    @GetMapping
    public List<EsgProfileResponse> list() {
        return esgService.findAll();
    }

    @GetMapping("/by-flat/{flatId}")
    public EsgProfileResponse getByFlat(@PathVariable UUID flatId) {
        return esgService.findByFlatId(flatId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    public EsgProfileResponse upsert(@Valid @RequestBody UpsertEsgProfileRequest request) {
        return esgService.upsert(request);
    }
}
