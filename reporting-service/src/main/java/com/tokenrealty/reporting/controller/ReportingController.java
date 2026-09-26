package com.tokenrealty.reporting.controller;

import com.tokenrealty.reporting.dto.ReportingDtos.DividendsResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.OccupancyResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.RegulatoryExportResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.TradingSummaryResponse;
import com.tokenrealty.reporting.service.ReportingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingQueryService queryService;

    @GetMapping("/trading-summary")
    public TradingSummaryResponse tradingSummary() {
        return queryService.tradingSummary();
    }

    @GetMapping("/occupancy")
    public OccupancyResponse occupancy() {
        return queryService.occupancy();
    }

    @GetMapping("/dividends")
    public DividendsResponse dividends() {
        return queryService.dividends();
    }

    @GetMapping("/export")
    public RegulatoryExportResponse export(
            @RequestParam(defaultValue = "json") String format) {
        return queryService.regulatoryExport(format);
    }
}
