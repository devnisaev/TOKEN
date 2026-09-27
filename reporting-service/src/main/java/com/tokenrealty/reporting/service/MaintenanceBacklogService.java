package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.client.RentalClient;
import com.tokenrealty.reporting.client.RentalClient.MaintenanceTicketView;
import com.tokenrealty.reporting.dto.ReportingDtos.MaintenanceBacklogItem;
import com.tokenrealty.reporting.dto.ReportingDtos.MaintenanceBacklogSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceBacklogService {

    private static final int BACKLOG_THRESHOLD = 2;

    private final RentalClient rentalClient;
    private final Clock clock;

    public MaintenanceBacklogSummary summary() {
        List<MaintenanceBacklogItem> backlogFlats = backlogFlatItems();
        return new MaintenanceBacklogSummary(
                backlogFlats.size(),
                rentalClient.countOpenMaintenanceTickets(),
                backlogFlats,
                clock.instant());
    }

    public List<MaintenanceBacklogItem> backlogFlatItems() {
        Map<UUID, Long> openByFlat = rentalClient.listOpenMaintenanceTickets().stream()
                .filter(ticket -> ticket.flatId() != null)
                .collect(Collectors.groupingBy(MaintenanceTicketView::flatId, Collectors.counting()));
        return openByFlat.entrySet().stream()
                .filter(entry -> entry.getValue() >= BACKLOG_THRESHOLD)
                .sorted(Comparator.comparingLong(Map.Entry<UUID, Long>::getValue).reversed())
                .map(entry -> new MaintenanceBacklogItem(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }
}
