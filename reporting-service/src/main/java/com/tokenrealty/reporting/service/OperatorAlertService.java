package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.client.NotificationClient;
import com.tokenrealty.reporting.client.PropertyRegistryClient;
import com.tokenrealty.reporting.client.PropertyRegistryClient.InsuranceExpiryAlertView;
import com.tokenrealty.reporting.dto.ReportingDtos.GenerateOperatorAlertsResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.OperatorAlertItem;
import com.tokenrealty.reporting.entity.AssetHealthScoreRecord;
import com.tokenrealty.reporting.entity.EsgSnapshotRecord;
import com.tokenrealty.reporting.entity.OperatorAlertRecord;
import com.tokenrealty.reporting.entity.OperatorAlertRecord.AlertSeverity;
import com.tokenrealty.reporting.entity.OperatorAlertRecord.AlertStatus;
import com.tokenrealty.reporting.repository.AssetHealthScoreRecordRepository;
import com.tokenrealty.reporting.repository.EsgSnapshotRecordRepository;
import com.tokenrealty.reporting.repository.OperatorAlertRecordRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OperatorAlertService {

    private static final BigDecimal HEALTH_AT_RISK_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal LOW_OCCUPANCY_THRESHOLD = new BigDecimal("60.00");
    private static final int INSURANCE_CRITICAL_DAYS = 7;

    private final OperatorAlertRecordRepository operatorAlertRecordRepository;
    private final AssetHealthScoreRecordRepository assetHealthScoreRecordRepository;
    private final EsgSnapshotRecordRepository esgSnapshotRecordRepository;
    private final PropertyRegistryClient propertyRegistryClient;
    private final NotificationClient notificationClient;
    private final LeaseCoverageService leaseCoverageService;
    private final Clock clock;

    public Page<OperatorAlertItem> listOpen(Pageable pageable) {
        return listByStatus(AlertStatus.OPEN, pageable);
    }

    public Page<OperatorAlertItem> listAcknowledged(Pageable pageable) {
        return listByStatus(AlertStatus.ACKNOWLEDGED, pageable);
    }

    private Page<OperatorAlertItem> listByStatus(AlertStatus status, Pageable pageable) {
        return operatorAlertRecordRepository
                .findByStatusOrderByDetectedAtDesc(status, pageable)
                .map(this::toItem);
    }

    public long countOpen() {
        return operatorAlertRecordRepository.countByStatus(AlertStatus.OPEN);
    }

    @Transactional
    public GenerateOperatorAlertsResponse generate(int insuranceWithinDays) {
        Instant now = clock.instant();
        int created = 0;
        created += generateHealthAlerts(now);
        created += generateOccupancyAlerts(now);
        created += generateInsuranceAlerts(now, insuranceWithinDays);
        created += generateVacancyRiskAlerts(now);
        return new GenerateOperatorAlertsResponse(created, operatorAlertRecordRepository.countByStatus(AlertStatus.OPEN));
    }

    @Transactional
    public OperatorAlertItem acknowledge(UUID id) {
        OperatorAlertRecord record = operatorAlertRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OperatorAlert", id));
        record.setStatus(AlertStatus.ACKNOWLEDGED);
        return toItem(operatorAlertRecordRepository.save(record));
    }

    private int generateHealthAlerts(Instant now) {
        int created = 0;
        for (AssetHealthScoreRecord score : assetHealthScoreRecordRepository.findAll()) {
            if (score.getHealthScore().compareTo(HEALTH_AT_RISK_THRESHOLD) >= 0) {
                continue;
            }
            if (upsertAlert(
                    "HEALTH_AT_RISK:" + score.getId(),
                    "HEALTH_AT_RISK",
                    AlertSeverity.WARNING,
                    score.getId(),
                    score.getBuildingId(),
                    score.getFlatId(),
                    "Asset health score " + score.getHealthScore() + " below threshold",
                    now)) {
                created++;
            }
        }
        return created;
    }

    private int generateOccupancyAlerts(Instant now) {
        int created = 0;
        for (EsgSnapshotRecord snapshot : esgSnapshotRecordRepository.findAll()) {
            BigDecimal occupancy = snapshot.getOccupancyPct();
            if (occupancy == null || occupancy.compareTo(LOW_OCCUPANCY_THRESHOLD) >= 0) {
                continue;
            }
            if (upsertAlert(
                    "LOW_OCCUPANCY:" + snapshot.getId(),
                    "LOW_OCCUPANCY",
                    AlertSeverity.WARNING,
                    snapshot.getId(),
                    snapshot.getBuildingId(),
                    snapshot.getFlatId(),
                    "Occupancy " + occupancy + "% below " + LOW_OCCUPANCY_THRESHOLD + "%",
                    now)) {
                created++;
            }
        }
        return created;
    }

    private int generateVacancyRiskAlerts(Instant now) {
        int created = 0;
        for (UUID flatId : leaseCoverageService.vacancyRiskFlatIds()) {
            EsgSnapshotRecord snapshot = esgSnapshotRecordRepository.findAll().stream()
                    .filter(record -> flatId.equals(record.getFlatId()))
                    .max(java.util.Comparator.comparing(EsgSnapshotRecord::getSnapshotAt))
                    .orElse(null);
            if (snapshot == null) {
                continue;
            }
            if (upsertAlert(
                    "VACANCY_RISK:" + flatId,
                    "VACANCY_RISK",
                    AlertSeverity.WARNING,
                    snapshot.getId(),
                    snapshot.getBuildingId(),
                    flatId,
                    "Low occupancy " + snapshot.getOccupancyPct() + "% with no active lease",
                    now)) {
                created++;
            }
        }
        return created;
    }

    private int generateInsuranceAlerts(Instant now, int withinDays) {
        int created = 0;
        try {
            for (InsuranceExpiryAlertView alert : propertyRegistryClient.listExpiringInsurance(withinDays)) {
                AlertSeverity severity = alert.daysUntilExpiry() <= INSURANCE_CRITICAL_DAYS
                        ? AlertSeverity.CRITICAL
                        : AlertSeverity.WARNING;
                if (upsertAlert(
                        "INSURANCE_EXPIRING:" + alert.id(),
                        "INSURANCE_EXPIRING",
                        severity,
                        alert.id(),
                        alert.buildingId(),
                        alert.flatId(),
                        "Insurance policy " + alert.policyNumber() + " expires in "
                                + alert.daysUntilExpiry() + " days",
                        now)) {
                    created++;
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Skipping insurance expiry alerts: {}", ex.getMessage());
        }
        return created;
    }

    private boolean upsertAlert(
            String dedupeKey,
            String alertType,
            AlertSeverity severity,
            UUID referenceId,
            UUID buildingId,
            UUID flatId,
            String message,
            Instant detectedAt) {
        if (operatorAlertRecordRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return false;
        }
        operatorAlertRecordRepository.save(OperatorAlertRecord.builder()
                .dedupeKey(dedupeKey)
                .alertType(alertType)
                .severity(severity)
                .status(AlertStatus.OPEN)
                .referenceId(referenceId)
                .buildingId(buildingId)
                .flatId(flatId)
                .message(message)
                .detectedAt(detectedAt)
                .build());
        if (severity == AlertSeverity.CRITICAL) {
            notificationClient.sendOperatorAlert(alertType, severity.name(), message);
        }
        return true;
    }

    private OperatorAlertItem toItem(OperatorAlertRecord record) {
        return new OperatorAlertItem(
                record.getId(),
                record.getAlertType(),
                record.getSeverity().name(),
                record.getStatus().name(),
                record.getReferenceId(),
                record.getBuildingId(),
                record.getFlatId(),
                record.getMessage(),
                record.getDetectedAt());
    }
}
