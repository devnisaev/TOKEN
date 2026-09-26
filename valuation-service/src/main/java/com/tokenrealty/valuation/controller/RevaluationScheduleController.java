package com.tokenrealty.valuation.controller;

import com.tokenrealty.valuation.dto.ValuationDtos.CreateRevaluationScheduleRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.RevaluationScheduleResponse;
import com.tokenrealty.valuation.service.RevaluationScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/valuations/schedules")
@RequiredArgsConstructor
@Tag(name = "Revaluation schedules", description = "Periodic revaluation schedules per building or flat")
public class RevaluationScheduleController {

    private final RevaluationScheduleService revaluationScheduleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a periodic revaluation schedule (ADMIN)")
    public RevaluationScheduleResponse create(@Valid @RequestBody CreateRevaluationScheduleRequest request) {
        return revaluationScheduleService.create(request);
    }

    @GetMapping("/building/{buildingId}")
    @Operation(summary = "List revaluation schedules for a building")
    public List<RevaluationScheduleResponse> listByBuilding(@PathVariable UUID buildingId) {
        return revaluationScheduleService.findByBuilding(buildingId);
    }
}
