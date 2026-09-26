package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.ApprovedBuilding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApprovedBuildingRepository extends JpaRepository<ApprovedBuilding, UUID> {
}
