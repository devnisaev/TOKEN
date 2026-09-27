package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.AlertTypeCount;
import com.tokenrealty.reporting.dto.ReportingDtos.OperatorAlertSummaryResponse;
import com.tokenrealty.reporting.entity.OperatorAlertRecord;
import com.tokenrealty.reporting.repository.OperatorAlertRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperatorAlertSummaryService {

    private final OperatorAlertRecordRepository operatorAlertRecordRepository;
    private final Clock clock;

    public OperatorAlertSummaryResponse summary() {
        List<OperatorAlertRecord> alerts = operatorAlertRecordRepository.findAll();
        long openCount = alerts.stream()
                .filter(alert -> alert.getStatus() == OperatorAlertRecord.AlertStatus.OPEN)
                .count();
        long criticalOpen = alerts.stream()
                .filter(alert -> alert.getStatus() == OperatorAlertRecord.AlertStatus.OPEN)
                .filter(alert -> alert.getSeverity() == OperatorAlertRecord.AlertSeverity.CRITICAL)
                .count();

        List<AlertTypeCount> byType = alerts.stream()
                .collect(Collectors.groupingBy(OperatorAlertRecord::getAlertType, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new AlertTypeCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AlertTypeCount::alertType))
                .toList();

        return new OperatorAlertSummaryResponse(openCount, criticalOpen, byType, clock.instant());
    }
}
