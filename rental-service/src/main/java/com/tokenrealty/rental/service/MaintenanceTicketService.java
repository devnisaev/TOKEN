package com.tokenrealty.rental.service;

import com.tokenrealty.rental.dto.RentalDtos.*;
import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.MaintenanceTicket;
import com.tokenrealty.rental.repository.MaintenanceTicketRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceTicketService {

    private final MaintenanceTicketRepository ticketRepository;
    private final LeaseService leaseService;

    public List<MaintenanceTicketResponse> list(UUID tenantId, UUID leaseId) {
        if (tenantId != null) {
            return ticketRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (leaseId != null) {
            return ticketRepository.findByLeaseIdOrderByCreatedAtDesc(leaseId).stream()
                    .map(this::toResponse)
                    .toList();
        }
        throw new ValidationException("tenantId or leaseId query parameter is required");
    }

    public MaintenanceTicketResponse findById(UUID id) {
        return toResponse(getTicket(id));
    }

    @Transactional
    public MaintenanceTicketResponse create(CreateMaintenanceTicketRequest request, UUID callerTenantId) {
        Lease lease = leaseService.getLease(request.leaseId());
        if (callerTenantId != null && !lease.getTenantId().equals(callerTenantId)) {
            throw new ValidationException("Tenant may only open tickets for their own lease");
        }
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .leaseId(lease.getId())
                .flatId(lease.getFlatId())
                .tenantId(lease.getTenantId())
                .title(request.title())
                .description(request.description())
                .status(MaintenanceTicket.TicketStatus.OPEN)
                .build();
        return toResponse(ticketRepository.save(ticket));
    }

    MaintenanceTicket getTicket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceTicket", id));
    }

    private MaintenanceTicketResponse toResponse(MaintenanceTicket ticket) {
        return new MaintenanceTicketResponse(
                ticket.getId(),
                ticket.getLeaseId(),
                ticket.getFlatId(),
                ticket.getTenantId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt()
        );
    }
}
