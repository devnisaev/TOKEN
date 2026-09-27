package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.SurveillanceAlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SurveillanceAlertRecordRepository extends JpaRepository<SurveillanceAlertRecord, UUID> {
}
