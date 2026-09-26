package com.tokenrealty.rental.service;

import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.Lease.LeaseStatus;
import com.tokenrealty.rental.kafka.port.RentalEventPublisher;
import com.tokenrealty.rental.kafka.port.RentalEventPublisher.LeaseExpiredEvent;
import com.tokenrealty.rental.kafka.port.RentalEventPublisher.RentDueEvent;
import com.tokenrealty.rental.repository.LeaseRepository;
import com.tokenrealty.rental.repository.RentPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;

@Service
@Slf4j
@RequiredArgsConstructor
public class RentScheduleService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LeaseRepository leaseRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final RentalEventPublisher rentalEventPublisher;

    @Scheduled(cron = "${tokenrealty.rental.rent-due.cron:0 0 8 1 * *}")
    @Transactional(readOnly = true)
    public void publishMonthlyRentDue() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        String period = YearMonth.from(monthStart).format(PERIOD_FORMAT);

        leaseRepository.findByStatus(LeaseStatus.ACTIVE).stream()
                .filter(lease -> !lease.getStartDate().isAfter(monthEnd))
                .filter(lease -> lease.getEndDate() == null || !lease.getEndDate().isBefore(monthStart))
                .filter(lease -> !rentPaymentRepository.existsByLeaseIdAndPeriod(lease.getId(), period))
                .forEach(lease -> publishRentDue(lease, period, monthStart));
    }

    @Scheduled(cron = "${tokenrealty.rental.lease-expiry.cron:0 0 9 * * *}")
    @Transactional
    public void expireLeases() {
        LocalDate today = LocalDate.now();
        leaseRepository.findByStatusAndEndDateBefore(LeaseStatus.ACTIVE, today)
                .forEach(this::expireLease);
    }

    private void publishRentDue(Lease lease, String period, LocalDate dueDate) {
        rentalEventPublisher.publishRentDue(new RentDueEvent(
                lease.getId(),
                lease.getFlatId(),
                lease.getTenantId(),
                lease.getMonthlyRentUsd(),
                period,
                dueDate));
        log.info("Published rent due lease={} period={}", lease.getId(), period);
    }

    private void expireLease(Lease lease) {
        lease.setStatus(LeaseStatus.EXPIRED);
        leaseRepository.save(lease);
        rentalEventPublisher.publishLeaseExpired(new LeaseExpiredEvent(
                lease.getId(),
                lease.getFlatId(),
                lease.getTenantId(),
                lease.getEndDate(),
                Instant.now()));
        log.info("Lease {} expired endDate={}", lease.getId(), lease.getEndDate());
    }
}
