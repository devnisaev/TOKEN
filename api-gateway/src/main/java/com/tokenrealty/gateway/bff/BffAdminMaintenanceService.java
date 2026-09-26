package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.RentalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BffAdminMaintenanceService {

    private final RentalClient rentalClient;

    public List<RentalClient.MaintenanceTicketView> getMaintenanceQueue() {
        return rentalClient.listAllMaintenanceTickets();
    }
}
