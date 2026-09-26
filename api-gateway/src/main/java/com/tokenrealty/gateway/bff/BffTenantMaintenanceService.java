package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.PropertyRegistryClient;
import com.tokenrealty.gateway.client.RentalClient;
import com.tokenrealty.gateway.dto.BffDtos.TenantFlatSummary;
import com.tokenrealty.gateway.dto.BffDtos.TenantMaintenanceBffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BffTenantMaintenanceService {

    private final RentalClient rentalClient;
    private final PropertyRegistryClient registryClient;

    public List<TenantMaintenanceBffResponse> getTenantMaintenance(UUID tenantId) {
        return rentalClient.listLeasesByTenantId(tenantId).stream()
                .map(this::toMaintenanceAggregate)
                .toList();
    }

    private TenantMaintenanceBffResponse toMaintenanceAggregate(RentalClient.LeaseView lease) {
        var flat = registryClient.getFlat(lease.flatId());
        var openTickets = rentalClient.listMaintenanceTicketsByLeaseId(lease.id()).stream()
                .filter(ticket -> "OPEN".equals(ticket.status()))
                .toList();

        return TenantMaintenanceBffResponse.builder()
                .lease(lease)
                .flat(toFlatSummary(flat))
                .openTickets(openTickets)
                .build();
    }

    private static TenantFlatSummary toFlatSummary(PropertyRegistryClient.FlatView flat) {
        return TenantFlatSummary.builder()
                .flatId(flat.id())
                .buildingId(flat.buildingId())
                .buildingName(flat.buildingName())
                .flatNumber(flat.flatNumber())
                .floor(flat.floor())
                .areaSqm(flat.areaSqm())
                .status(flat.status())
                .build();
    }
}
