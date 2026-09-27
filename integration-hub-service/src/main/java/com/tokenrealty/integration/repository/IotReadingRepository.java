package com.tokenrealty.integration.repository;

import com.tokenrealty.integration.entity.IotReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IotReadingRepository extends JpaRepository<IotReading, UUID> {

    List<IotReading> findByFlatIdOrderByRecordedAtDesc(UUID flatId);
}
