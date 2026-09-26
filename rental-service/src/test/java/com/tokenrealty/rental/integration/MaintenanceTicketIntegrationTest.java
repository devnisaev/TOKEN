package com.tokenrealty.rental.integration;

import com.tokenrealty.rental.dto.RentalDtos.CreateMaintenanceTicketRequest;
import com.tokenrealty.rental.dto.RentalDtos.UpdateMaintenanceTicketStatusRequest;
import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.MaintenanceTicket;
import com.tokenrealty.rental.repository.LeaseRepository;
import com.tokenrealty.rental.repository.MaintenanceTicketRepository;
import com.tokenrealty.rental.service.MaintenanceTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Maintenance ticket integration test")
class MaintenanceTicketIntegrationTest {

    @Autowired MaintenanceTicketService maintenanceTicketService;
    @Autowired MaintenanceTicketRepository ticketRepository;
    @Autowired LeaseRepository leaseRepository;

    private UUID leaseId;
    private UUID tenantId;

    @BeforeEach
    void seedLease() {
        tenantId = UUID.randomUUID();
        Lease lease = leaseRepository.save(Lease.builder()
                .flatId(UUID.randomUUID())
                .tenantId(tenantId)
                .tenantWallet("0xTenant")
                .spvRecipientId(UUID.randomUUID())
                .spvWallet("0xSPV")
                .monthlyRentUsd(new BigDecimal("650.00"))
                .startDate(LocalDate.now().withDayOfMonth(1))
                .endDate(LocalDate.now().plusYears(1))
                .status(Lease.LeaseStatus.ACTIVE)
                .build());
        leaseId = lease.getId();
    }

    @Test
    @DisplayName("create and update maintenance ticket status")
    void createAndUpdateStatus() {
        var created = maintenanceTicketService.create(
                new CreateMaintenanceTicketRequest(leaseId, "Leaky faucet", "Kitchen sink dripping"),
                tenantId);

        assertThat(created.status()).isEqualTo(MaintenanceTicket.TicketStatus.OPEN);

        var updated = maintenanceTicketService.updateStatus(
                created.id(),
                new UpdateMaintenanceTicketStatusRequest(MaintenanceTicket.TicketStatus.IN_PROGRESS));

        assertThat(updated.status()).isEqualTo(MaintenanceTicket.TicketStatus.IN_PROGRESS);
        assertThat(ticketRepository.findById(created.id()).orElseThrow().getStatus())
                .isEqualTo(MaintenanceTicket.TicketStatus.IN_PROGRESS);
    }
}
