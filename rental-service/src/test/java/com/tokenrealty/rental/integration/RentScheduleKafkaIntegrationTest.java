package com.tokenrealty.rental.integration;

import com.tokenrealty.outbox.OutboxStatus;
import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.Lease.LeaseStatus;
import com.tokenrealty.rental.kafka.RentalKafkaEventTypes;
import com.tokenrealty.rental.kafka.outbox.OutboxEventRepository;
import com.tokenrealty.rental.repository.LeaseRepository;
import com.tokenrealty.rental.service.RentScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "tokenrealty.kafka.enabled=true")
@DisplayName("Rent schedule Kafka outbox integration test")
class RentScheduleKafkaIntegrationTest {

    @Autowired RentScheduleService rentScheduleService;
    @Autowired LeaseRepository leaseRepository;
    @Autowired OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void clearOutbox() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("publishMonthlyRentDue creates rent.due outbox row")
    void publishMonthlyRentDue_createsOutboxRow() {
        UUID leaseId = seedActiveLease(LocalDate.now().minusMonths(2), LocalDate.now().plusYears(1));

        rentScheduleService.publishMonthlyRentDue();

        assertThat(outboxEventRepository.findAll())
                .anyMatch(event -> event.getEventType().equals(RentalKafkaEventTypes.RENT_DUE)
                        && event.getAggregateId().equals(leaseId)
                        && event.getStatus() == OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("expireLeases creates lease.expired outbox row")
    void expireLeases_createsOutboxRow() {
        UUID leaseId = seedActiveLease(LocalDate.now().minusYears(1), LocalDate.now().minusDays(1));

        rentScheduleService.expireLeases();

        assertThat(leaseRepository.findById(leaseId).orElseThrow().getStatus())
                .isEqualTo(LeaseStatus.EXPIRED);
        assertThat(outboxEventRepository.findAll())
                .anyMatch(event -> event.getEventType().equals(RentalKafkaEventTypes.LEASE_EXPIRED)
                        && event.getAggregateId().equals(leaseId)
                        && event.getStatus() == OutboxStatus.PENDING);
    }

    private UUID seedActiveLease(LocalDate startDate, LocalDate endDate) {
        Lease lease = leaseRepository.save(Lease.builder()
                .flatId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .tenantWallet("0xTenant")
                .spvRecipientId(UUID.randomUUID())
                .spvWallet("0xSPV")
                .monthlyRentUsd(new BigDecimal("850.00"))
                .startDate(startDate)
                .endDate(endDate)
                .status(LeaseStatus.ACTIVE)
                .build());
        return lease.getId();
    }
}
