package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.dto.BffDtos.BuildingBffDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.FlatDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.ListingDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.PortfolioBffResponse;
import com.tokenrealty.gateway.dto.BffDtos.TenantLeaseBffResponse;
import com.tokenrealty.gateway.dto.BffDtos.TenantMaintenanceBffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
public class BffController {

    private final BffFlatService flatService;
    private final BffListingService listingService;
    private final BffBuildingService buildingService;
    private final BffPortfolioService portfolioService;
    private final BffTenantLeaseService tenantLeaseService;
    private final BffTenantMaintenanceService tenantMaintenanceService;

    @GetMapping("/flats/{flatId}")
    public FlatDetailResponse flatDetail(@PathVariable UUID flatId) {
        return flatService.getFlatDetail(flatId);
    }

    @GetMapping("/listings/{listingId}")
    public ListingDetailResponse listingDetail(@PathVariable UUID listingId) {
        return listingService.getListingDetail(listingId);
    }

    @GetMapping("/buildings/{buildingId}")
    public BuildingBffDetailResponse buildingDetail(@PathVariable UUID buildingId) {
        return buildingService.getBuildingDetail(buildingId);
    }

    @GetMapping("/investors/{investorId}/portfolio")
    public PortfolioBffResponse portfolio(@PathVariable UUID investorId) {
        return portfolioService.getPortfolio(investorId);
    }

    @GetMapping("/tenants/{tenantId}/lease")
    public List<TenantLeaseBffResponse> tenantLeases(@PathVariable UUID tenantId) {
        return tenantLeaseService.getTenantLeases(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/maintenance")
    public List<TenantMaintenanceBffResponse> tenantMaintenance(@PathVariable UUID tenantId) {
        return tenantMaintenanceService.getTenantMaintenance(tenantId);
    }
}
