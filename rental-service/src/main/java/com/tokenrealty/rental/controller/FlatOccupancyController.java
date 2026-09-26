package com.tokenrealty.rental.controller;

import com.tokenrealty.rental.dto.RentalDtos.OccupancyResponse;
import com.tokenrealty.rental.service.LeaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/occupancy")
@RequiredArgsConstructor
public class FlatOccupancyController {

    private final LeaseService leaseService;

    @GetMapping("/flats/{flatId}")
    public OccupancyResponse occupancy(@PathVariable UUID flatId) {
        return leaseService.getOccupancy(flatId);
    }
}
