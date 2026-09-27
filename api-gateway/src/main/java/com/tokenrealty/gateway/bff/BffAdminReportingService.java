package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.ReportingClient;
import com.tokenrealty.gateway.dto.BffDtos.AdminReportsSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BffAdminReportingService {

    private final ReportingClient reportingClient;

    public AdminReportsSummaryResponse getAdminReportsSummary() {
        var trading = reportingClient.tradingSummary();
        var occupancy = reportingClient.occupancy();
        var dividends = reportingClient.dividends();
        var taxPage = reportingClient.taxSummaries(null, PageRequest.of(0, 500));
        var alertPage = reportingClient.surveillanceAlerts(PageRequest.of(0, 1));
        BigDecimal ytdWithholding = taxPage.content().stream()
                .map(ReportingClient.TaxSummaryView::withholdingAmountUsd)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return AdminReportsSummaryResponse.builder()
                .settledTradeCount(trading.settledTradeCount())
                .matchedOrderCount(trading.matchedOrderCount())
                .matchedVolumeUsd(trading.matchedVolumeUsd())
                .tokenizedFlatCount(occupancy.tokenizedFlatCount())
                .occupiedFlatCount(occupancy.occupiedFlatCount())
                .occupancyRate(occupancy.occupancyRate())
                .totalDistributedUsd(dividends.totalDistributedUsd())
                .recentDividendCount(dividends.dividends() == null ? 0 : dividends.dividends().size())
                .openSurveillanceAlerts(alertPage.totalElements())
                .ytdWithholdingUsd(ytdWithholding)
                .generatedAt(trading.generatedAt())
                .build();
    }
}
