package com.tokenrealty.gateway.graphql;

import com.tokenrealty.gateway.bff.*;
import com.tokenrealty.gateway.dto.BffDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class BffGraphQlController {

    private final BffFlatService flatService;
    private final BffListingService listingService;
    private final BffPortfolioService portfolioService;
    private final BffTenantLeaseService tenantLeaseService;
    private final BffTenantMaintenanceService tenantMaintenanceService;

    @QueryMapping
    public FlatDetailResponse flatDetail(@Argument String flatId) {
        return flatService.getFlatDetail(UUID.fromString(flatId));
    }

    @QueryMapping
    public ListingDetailResponse listingDetail(@Argument String listingId) {
        return listingService.getListingDetail(UUID.fromString(listingId));
    }

    @QueryMapping
    public PortfolioBffResponse investorPortfolio(@Argument String investorId) {
        return portfolioService.getPortfolio(UUID.fromString(investorId));
    }

    @QueryMapping
    public List<TenantLeaseBffResponse> tenantLeases(@Argument String tenantId) {
        return tenantLeaseService.getTenantLeases(UUID.fromString(tenantId));
    }

    @QueryMapping
    public List<TenantMaintenanceBffResponse> tenantMaintenance(@Argument String tenantId) {
        return tenantMaintenanceService.getTenantMaintenance(UUID.fromString(tenantId));
    }
}
