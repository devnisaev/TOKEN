package com.tokenrealty.integration.controller;

import com.tokenrealty.integration.dto.IntegrationDtos.*;
import com.tokenrealty.integration.service.BridgeTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/integrations/bridge")
@RequiredArgsConstructor
public class BridgeController {

    private final BridgeTransferService bridgeTransferService;

    @GetMapping("/transfers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BridgeTransferResponse> listPending() {
        return bridgeTransferService.listPending();
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    public BridgeTransferResponse create(@Valid @RequestBody CreateBridgeTransferRequest request) {
        return bridgeTransferService.create(request);
    }

    @PostMapping("/transfers/{id}/relay")
    @PreAuthorize("hasRole('ADMIN')")
    public BridgeTransferResponse relay(@PathVariable UUID id) {
        return bridgeTransferService.relay(id);
    }
}
