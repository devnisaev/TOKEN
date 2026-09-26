package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.dto.BffDtos.FlatDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.ListingDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
public class BffController {

    private final BffFlatService flatService;
    private final BffListingService listingService;

    @GetMapping("/flats/{flatId}")
    public FlatDetailResponse flatDetail(@PathVariable UUID flatId) {
        return flatService.getFlatDetail(flatId);
    }

    @GetMapping("/listings/{listingId}")
    public ListingDetailResponse listingDetail(@PathVariable UUID listingId) {
        return listingService.getListingDetail(listingId);
    }
}
