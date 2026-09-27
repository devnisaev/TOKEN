package com.tokenrealty.integration.controller;

import com.tokenrealty.integration.dto.IntegrationDtos.*;
import com.tokenrealty.integration.service.IotFeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/integrations/iot")
@RequiredArgsConstructor
public class IotController {

    private final IotFeedService iotFeedService;

    @PostMapping("/readings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    public IotReadingResponse ingest(@Valid @RequestBody IngestIotReadingRequest request) {
        return iotFeedService.ingest(request);
    }

    @GetMapping("/readings")
    public List<IotReadingResponse> listByFlat(@RequestParam UUID flatId) {
        return iotFeedService.listByFlat(flatId);
    }
}
