package com.tokenrealty.valuation.integration;

import com.tokenrealty.valuation.dto.ValuationDtos.CreateRevaluationScheduleRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.SubmitValuationRequest;
import com.tokenrealty.valuation.entity.RevaluationSchedule;
import com.tokenrealty.valuation.entity.ValuationRequestStatus;
import com.tokenrealty.valuation.repository.RevaluationScheduleRepository;
import com.tokenrealty.valuation.repository.ValuationRequestRepository;
import com.tokenrealty.valuation.service.RevaluationScheduleService;
import com.tokenrealty.valuation.service.ValuationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Revaluation schedule integration test")
class RevaluationScheduleTest {

    @Autowired RevaluationScheduleService revaluationScheduleService;
    @Autowired ValuationService valuationService;
    @Autowired RevaluationScheduleRepository scheduleRepository;
    @Autowired ValuationRequestRepository valuationRequestRepository;

    @Test
    void dueSchedule_createsPendingValuationRequestAndAdvancesNextDueAt() {
        UUID adminId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        valuationService.submit(
                new SubmitValuationRequest(buildingId, flatId, new BigDecimal("500000.00"), 500L, "Initial"),
                adminId);
        valuationService.findByBuilding(buildingId).stream()
                .filter(request -> request.status() == ValuationRequestStatus.PENDING)
                .findFirst()
                .ifPresent(request -> valuationService.approve(request.id(), adminId));

        Instant dueAt = Instant.now().minus(1, ChronoUnit.DAYS);
        RevaluationSchedule schedule = scheduleRepository.save(RevaluationSchedule.builder()
                .buildingId(buildingId)
                .flatId(flatId)
                .intervalMonths(6)
                .nextDueAt(dueAt)
                .active(true)
                .build());

        long requestsBefore = valuationRequestRepository.count();
        revaluationScheduleService.processDueSchedules();

        assertThat(valuationRequestRepository.count()).isEqualTo(requestsBefore + 1);
        assertThat(valuationRequestRepository.findAll()).anySatisfy(request -> {
            assertThat(request.getStatus()).isEqualTo(ValuationRequestStatus.PENDING);
            assertThat(request.getNotes()).isEqualTo("Scheduled revaluation");
            assertThat(request.getSubmittedBy()).isEqualTo(RevaluationScheduleService.SYSTEM_SUBMITTER_ID);
            assertThat(request.getFlatId()).isEqualTo(flatId);
        });

        RevaluationSchedule updated = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(updated.getNextDueAt()).isAfter(dueAt);
    }

    @Test
    void createSchedule_persistsAndListsByBuilding() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        var created = revaluationScheduleService.create(
                new CreateRevaluationScheduleRequest(buildingId, flatId, 12, null));

        assertThat(created.buildingId()).isEqualTo(buildingId);
        assertThat(created.flatId()).isEqualTo(flatId);
        assertThat(created.intervalMonths()).isEqualTo(12);
        assertThat(created.active()).isTrue();
        assertThat(created.nextDueAt()).isNotNull();

        assertThat(revaluationScheduleService.findByBuilding(buildingId))
                .singleElement()
                .returns(created.id(), from -> from.id());
    }
}
