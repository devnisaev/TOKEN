package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.ReportingClient;
import com.tokenrealty.gateway.dto.BffDtos.AdminReportsSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BffAdminReportingService {

    private final ReportingClient reportingClient;

    public AdminReportsSummaryResponse getAdminReportsSummary() {
        var trading = reportingClient.tradingSummary();
        var occupancy = reportingClient.occupancy();
        var dividends = reportingClient.dividends();
        return AdminReportsSummaryResponse.builder()
                .settledTradeCount(trading.settledTradeCount())
                .matchedOrderCount(trading.matchedOrderCount())
                .matchedVolumeUsd(trading.matchedVolumeUsd())
                .tokenizedFlatCount(occupancy.tokenizedFlatCount())
                .occupiedFlatCount(occupancy.occupiedFlatCount())
                .occupancyRate(occupancy.occupancyRate())
                .totalDistributedUsd(dividends.totalDistributedUsd())
                .recentDividendCount(dividends.dividends() == null ? 0 : dividends.dividends().size())
                .generatedAt(trading.generatedAt())
                .build();
    }
}
