package com.tokenrealty.valuation.repository;

import com.tokenrealty.valuation.entity.RevaluationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RevaluationScheduleRepository extends JpaRepository<RevaluationSchedule, UUID> {

    List<RevaluationSchedule> findByBuildingIdOrderByCreatedAtDesc(UUID buildingId);

    List<RevaluationSchedule> findByActiveTrueAndNextDueAtLessThanEqual(Instant dueBefore);
}
