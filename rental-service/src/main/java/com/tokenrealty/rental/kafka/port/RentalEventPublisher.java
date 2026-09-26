package com.tokenrealty.rental.kafka.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface RentalEventPublisher {

    void publishRentDue(RentDueEvent event);

    void publishLeaseExpired(LeaseExpiredEvent event);

    record RentDueEvent(
            UUID leaseId,
            UUID flatId,
            UUID tenantId,
            BigDecimal amountUsd,
            String period,
            LocalDate dueDate
    ) {
    }

    record LeaseExpiredEvent(
            UUID leaseId,
            UUID flatId,
            UUID tenantId,
            LocalDate endDate,
            Instant expiredAt
    ) {
    }
}
