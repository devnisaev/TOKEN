package com.tokenrealty.registry.repository;

import com.tokenrealty.registry.entity.SpvEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpvRepository extends JpaRepository<SpvEntity, UUID> {

    Optional<SpvEntity> findByBuildingId(UUID buildingId);

    Optional<SpvEntity> findByRegistrationNumber(String registrationNumber);

    Optional<SpvEntity> findByWalletAddress(String walletAddress);

    boolean existsByRegistrationNumber(String registrationNumber);
}
