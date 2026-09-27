package com.tokenrealty.registry.repository;

import com.tokenrealty.registry.entity.EsgProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EsgProfileRepository extends JpaRepository<EsgProfile, UUID> {

    Optional<EsgProfile> findByFlatId(UUID flatId);

    List<EsgProfile> findByBuildingId(UUID buildingId);
}
