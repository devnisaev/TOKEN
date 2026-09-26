package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.PropertyRegistryClient;
import com.tokenrealty.gateway.client.RentalClient;
import com.tokenrealty.gateway.dto.BffDtos.TenantFlatSummary;
import com.tokenrealty.gateway.dto.BffDtos.TenantLeaseBffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BffTenantLeaseService {

    private final RentalClient rentalClient;
    private final PropertyRegistryClient registryClient;

    public List<TenantLeaseBffResponse> getTenantLeases(UUID tenantId) {
        return rentalClient.listLeasesByTenantId(tenantId).stream()
                .map(this::toLeaseAggregate)
                .toList();
    }

    private TenantLeaseBffResponse toLeaseAggregate(RentalClient.LeaseView lease) {
        var flat = registryClient.getFlat(lease.flatId());
        var payments = rentalClient.listRentPayments(lease.id());
        var currentPeriod = YearMonth.now().toString();
        var rentDue = payments.stream()
                .noneMatch(payment -> currentPeriod.equals(payment.period()));

        return TenantLeaseBffResponse.builder()
                .lease(lease)
                .flat(toFlatSummary(flat))
                .rentDue(rentDue)
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
