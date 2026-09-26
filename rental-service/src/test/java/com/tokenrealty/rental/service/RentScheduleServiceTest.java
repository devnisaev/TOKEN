package com.tokenrealty.rental.service;

import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.Lease.LeaseStatus;
import com.tokenrealty.rental.kafka.port.RentalEventPublisher;
import com.tokenrealty.rental.kafka.port.RentalEventPublisher.LeaseExpiredEvent;
import com.tokenrealty.rental.kafka.port.RentalEventPublisher.RentDueEvent;
import com.tokenrealty.rental.repository.LeaseRepository;
import com.tokenrealty.rental.repository.RentPaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentScheduleService unit tests")
class RentScheduleServiceTest {

    @Mock LeaseRepository leaseRepository;
    @Mock RentPaymentRepository rentPaymentRepository;
    @Mock RentalEventPublisher rentalEventPublisher;
    @InjectMocks RentScheduleService rentScheduleService;

    @Test
    @DisplayName("publishMonthlyRentDue publishes for active leases without payment")
    void publishMonthlyRentDue_publishesForEligibleLeases() {
        UUID leaseId = UUID.randomUUID();
        Lease lease = Lease.builder()
                .flatId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .tenantWallet("0xTENANT")
                .spvRecipientId(UUID.randomUUID())
                .spvWallet("0xSPV")
                .monthlyRentUsd(new BigDecimal("1200.00"))
                .startDate(LocalDate.now().minusMonths(2))
                .status(LeaseStatus.ACTIVE)
                .build();
        lease.setId(leaseId);

        when(leaseRepository.findByStatus(LeaseStatus.ACTIVE)).thenReturn(List.of(lease));
        when(rentPaymentRepository.existsByLeaseIdAndPeriod(eq(leaseId), anyString())).thenReturn(false);

        rentScheduleService.publishMonthlyRentDue();

        ArgumentCaptor<RentDueEvent> captor = ArgumentCaptor.forClass(RentDueEvent.class);
        verify(rentalEventPublisher).publishRentDue(captor.capture());
        assertThat(captor.getValue().leaseId()).isEqualTo(leaseId);
        assertThat(captor.getValue().amountUsd()).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("expireLeases marks lease EXPIRED and publishes lease.expired")
    void expireLeases_publishesLeaseExpired() {
        UUID leaseId = UUID.randomUUID();
        LocalDate endDate = LocalDate.now().minusDays(1);
        Lease lease = Lease.builder()
                .flatId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .tenantWallet("0xTENANT")
                .spvRecipientId(UUID.randomUUID())
                .spvWallet("0xSPV")
                .monthlyRentUsd(new BigDecimal("900.00"))
                .startDate(LocalDate.now().minusYears(1))
                .endDate(endDate)
                .status(LeaseStatus.ACTIVE)
                .build();
        lease.setId(leaseId);

        when(leaseRepository.findByStatusAndEndDateBefore(LeaseStatus.ACTIVE, LocalDate.now()))
                .thenReturn(List.of(lease));
        when(leaseRepository.save(lease)).thenReturn(lease);

        rentScheduleService.expireLeases();

        assertThat(lease.getStatus()).isEqualTo(LeaseStatus.EXPIRED);
        verify(rentalEventPublisher).publishLeaseExpired(any(LeaseExpiredEvent.class));
    }
}
