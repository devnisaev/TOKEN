package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.RentalClient;
import com.tokenrealty.gateway.dto.BffDtos.AdminReportsSummaryResponse;
import com.tokenrealty.gateway.dto.BffDtos.BuildingBffDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.FlatDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.ListingDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.PortfolioBffResponse;
import com.tokenrealty.gateway.dto.BffDtos.TenantLeaseBffResponse;
import com.tokenrealty.gateway.dto.BffDtos.TenantMaintenanceBffResponse;
import com.tokenrealty.gateway.client.SearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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
    private final BffAdminMaintenanceService adminMaintenanceService;
    private final BffAdminReportingService adminReportingService;
    private final BffSearchService searchService;

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

    @GetMapping("/admin/maintenance-tickets")
    public List<RentalClient.MaintenanceTicketView> adminMaintenanceQueue() {
        return adminMaintenanceService.getMaintenanceQueue();
    }

    @GetMapping("/admin/reports/summary")
    public AdminReportsSummaryResponse adminReportsSummary() {
        return adminReportingService.getAdminReportsSummary();
    }

    @GetMapping("/search/listings")
    public SearchClient.SpringPage<SearchClient.ListingSearchResult> searchListings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20) Pageable pageable) {
        return searchService.searchListings(q, listingType, minPrice, maxPrice, pageable);
    }

    @GetMapping("/search/buildings")
    public SearchClient.SpringPage<SearchClient.BuildingSearchResult> searchBuildings(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return searchService.searchBuildings(q, pageable);
    }
}
