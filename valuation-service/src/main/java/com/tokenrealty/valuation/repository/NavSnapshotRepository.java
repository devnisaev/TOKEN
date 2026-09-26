package com.tokenrealty.valuation.repository;

import com.tokenrealty.valuation.entity.NavSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NavSnapshotRepository extends JpaRepository<NavSnapshot, UUID> {

    Optional<NavSnapshot> findTopByFlatIdOrderByApprovedAtDesc(UUID flatId);
}
