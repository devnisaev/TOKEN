package com.tokenrealty.valuation.service;

import com.tokenrealty.valuation.dto.ValuationDtos.CreateRevaluationScheduleRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.RevaluationScheduleResponse;
import com.tokenrealty.valuation.dto.ValuationDtos.SubmitValuationRequest;
import com.tokenrealty.valuation.entity.NavSnapshot;
import com.tokenrealty.valuation.entity.RevaluationSchedule;
import com.tokenrealty.valuation.entity.ValuationRequest;
import com.tokenrealty.valuation.entity.ValuationRequestStatus;
import com.tokenrealty.valuation.repository.NavSnapshotRepository;
import com.tokenrealty.valuation.repository.RevaluationScheduleRepository;
import com.tokenrealty.valuation.repository.ValuationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RevaluationScheduleService {

    public static final UUID SYSTEM_SUBMITTER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String SCHEDULED_NOTES = "Scheduled revaluation";

    private final RevaluationScheduleRepository scheduleRepository;
    private final NavSnapshotRepository navSnapshotRepository;
    private final ValuationRequestRepository valuationRequestRepository;
    private final ValuationService valuationService;
    private final Clock clock;

    @Transactional
    public RevaluationScheduleResponse create(CreateRevaluationScheduleRequest request) {
        Instant nextDueAt = request.nextDueAt() != null ? request.nextDueAt() : clock.instant();
        RevaluationSchedule schedule = scheduleRepository.save(RevaluationSchedule.builder()
                .buildingId(request.buildingId())
                .flatId(request.flatId())
                .intervalMonths(request.intervalMonths())
                .nextDueAt(nextDueAt)
                .active(true)
                .build());
        return toResponse(schedule);
    }

    public List<RevaluationScheduleResponse> findByBuilding(UUID buildingId) {
        return scheduleRepository.findByBuildingIdOrderByCreatedAtDesc(buildingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public int processDueSchedules() {
        Instant now = clock.instant();
        List<RevaluationSchedule> dueSchedules =
                scheduleRepository.findByActiveTrueAndNextDueAtLessThanEqual(now);
        dueSchedules.forEach(this::processOne);
        return dueSchedules.size();
    }

    private void processOne(RevaluationSchedule schedule) {
        Optional<SubmitValuationRequest> submitRequest = resolveSubmitRequest(schedule);
        if (submitRequest.isEmpty()) {
            log.warn(
                    "Skipping scheduled revaluation id={} buildingId={} flatId={}: no baseline valuation",
                    schedule.getId(),
                    schedule.getBuildingId(),
                    schedule.getFlatId());
            schedule.setNextDueAt(plusMonths(clock.instant(), schedule.getIntervalMonths()));
            scheduleRepository.save(schedule);
            return;
        }

        valuationService.submit(submitRequest.get(), SYSTEM_SUBMITTER_ID);
        schedule.setNextDueAt(plusMonths(clock.instant(), schedule.getIntervalMonths()));
        scheduleRepository.save(schedule);
        log.info(
                "Scheduled revaluation submitted for buildingId={} flatId={}",
                schedule.getBuildingId(),
                schedule.getFlatId());
    }

    private Optional<SubmitValuationRequest> resolveSubmitRequest(RevaluationSchedule schedule) {
        if (schedule.getFlatId() != null) {
            return navSnapshotRepository.findTopByFlatIdOrderByApprovedAtDesc(schedule.getFlatId())
                    .map(nav -> toSubmitRequest(schedule.getBuildingId(), nav))
                    .or(() -> latestApprovedRequest(schedule.getBuildingId(), schedule.getFlatId()));
        }
        return latestApprovedRequest(schedule.getBuildingId(), null);
    }

    private Optional<SubmitValuationRequest> latestApprovedRequest(UUID buildingId, UUID flatId) {
        return valuationRequestRepository.findByBuildingIdOrderByCreatedAtDesc(buildingId).stream()
                .filter(request -> request.getStatus() == ValuationRequestStatus.APPROVED)
                .filter(request -> flatId == null || flatId.equals(request.getFlatId()))
                .findFirst()
                .map(this::toSubmitRequest);
    }

    private SubmitValuationRequest toSubmitRequest(UUID buildingId, NavSnapshot nav) {
        return new SubmitValuationRequest(
                buildingId,
                nav.getFlatId(),
                nav.getValueUsd(),
                nav.getTotalTokens(),
                SCHEDULED_NOTES);
    }

    private SubmitValuationRequest toSubmitRequest(ValuationRequest request) {
        return new SubmitValuationRequest(
                request.getBuildingId(),
                request.getFlatId(),
                request.getValueUsd(),
                request.getTotalTokens(),
                SCHEDULED_NOTES);
    }

    private Instant plusMonths(Instant instant, int months) {
        ZonedDateTime zoned = instant.atZone(clock.getZone());
        return zoned.plusMonths(months).toInstant();
    }

    private RevaluationScheduleResponse toResponse(RevaluationSchedule schedule) {
        return new RevaluationScheduleResponse(
                schedule.getId(),
                schedule.getBuildingId(),
                schedule.getFlatId(),
                schedule.getIntervalMonths(),
                schedule.getNextDueAt(),
                schedule.isActive(),
                schedule.getCreatedAt());
    }
}
