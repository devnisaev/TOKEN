package com.tokenrealty.reporting.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class RentalClient extends DownstreamRestClientSupport {

    public RentalClient(@Qualifier("rentalRestClient") RestClient restClient) {
        super(restClient);
    }

    public long countOpenMaintenanceTickets() {
        try {
            List<MaintenanceTicketView> tickets = get(
                    uriBuilder -> uriBuilder.path("/v1/maintenance-tickets").build(),
                    new ParameterizedTypeReference<List<MaintenanceTicketView>>() {},
                    DownstreamServices.RENTAL);
            return tickets.stream().filter(t -> "OPEN".equals(t.status()) || "IN_PROGRESS".equals(t.status())).count();
        } catch (RuntimeException ex) {
            log.warn("Skipping maintenance ticket count: {}", ex.getMessage());
            return 0L;
        }
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
