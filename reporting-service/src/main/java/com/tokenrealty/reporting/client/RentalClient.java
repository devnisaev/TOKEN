package com.tokenrealty.reporting.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RentalClient extends DownstreamRestClientSupport {

    public RentalClient(@Qualifier("rentalRestClient") RestClient restClient) {
        super(restClient);
    }

    public List<LeaseView> listActiveLeases() {
        try {
            return get(
                    uriBuilder -> uriBuilder.path("/v1/leases/active").build(),
                    new ParameterizedTypeReference<List<LeaseView>>() {},
                    DownstreamServices.RENTAL);
        } catch (RuntimeException ex) {
            log.warn("Skipping active lease fetch: {}", ex.getMessage());
            return List.of();
        }
    }

    public List<LeaseExpiryView> listExpiringLeases(int withinDays) {
        try {
            return get(
                    uriBuilder -> uriBuilder
                            .path("/v1/leases/expiring")
                            .queryParam("withinDays", withinDays)
                            .build(),
                    new ParameterizedTypeReference<List<LeaseExpiryView>>() {},
                    DownstreamServices.RENTAL);
        } catch (RuntimeException ex) {
            log.warn("Skipping expiring lease fetch: {}", ex.getMessage());
            return List.of();
        }
    }

    public Set<UUID> activeLeaseFlatIds() {
        return listActiveLeases().stream().map(LeaseView::flatId).collect(Collectors.toSet());
    }

    public List<MaintenanceTicketView> listOpenMaintenanceTickets() {
        try {
            List<MaintenanceTicketView> tickets = get(
                    uriBuilder -> uriBuilder.path("/v1/maintenance-tickets").build(),
                    new ParameterizedTypeReference<List<MaintenanceTicketView>>() {},
                    DownstreamServices.RENTAL);
            return tickets.stream()
                    .filter(t -> "OPEN".equals(t.status()) || "IN_PROGRESS".equals(t.status()))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Skipping maintenance ticket fetch: {}", ex.getMessage());
            return List.of();
        }
    }

    public long countOpenMaintenanceTickets() {
        return listOpenMaintenanceTickets().size();
    }

    public record LeaseView(
            UUID id,
            UUID flatId,
            UUID tenantId,
            String tenantWallet,
            UUID spvRecipientId,
            String spvWallet,
            java.math.BigDecimal monthlyRentUsd,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            java.time.Instant createdAt) {
    }

    public record LeaseExpiryView(
            UUID id,
            UUID flatId,
            UUID tenantId,
            LocalDate endDate,
            long daysUntilExpiry) {
    }

    public record MaintenanceTicketView(
            UUID id,
            UUID leaseId,
            UUID flatId,
            UUID tenantId,
            String title,
            String description,
            String status,
            java.time.Instant createdAt) {
    }
}
