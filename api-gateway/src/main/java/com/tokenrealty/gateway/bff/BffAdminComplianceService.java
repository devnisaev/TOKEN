package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.ReportingClient;
import com.tokenrealty.gateway.dto.BffDtos.AdminComplianceReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BffAdminComplianceService {

    private final ReportingClient reportingClient;

    public AdminComplianceReportResponse getComplianceReport(Pageable pageable) {
        var taxSummaries = reportingClient.taxSummaries(null, pageable);
        var surveillanceAlerts = reportingClient.surveillanceAlerts(pageable);
        return AdminComplianceReportResponse.builder()
                .taxSummaries(taxSummaries.content())
                .taxSummaryTotal(taxSummaries.totalElements())
                .surveillanceAlerts(surveillanceAlerts.content())
                .surveillanceAlertTotal(surveillanceAlerts.totalElements())
                .build();
    }
}
