package com.tokenrealty.rental.repository;

import com.tokenrealty.rental.entity.MaintenanceTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID> {

    List<MaintenanceTicket> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<MaintenanceTicket> findByLeaseIdOrderByCreatedAtDesc(UUID leaseId);

    List<MaintenanceTicket> findAllByOrderByCreatedAtDesc();
}
