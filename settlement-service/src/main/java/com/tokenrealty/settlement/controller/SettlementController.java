package com.tokenrealty.settlement.controller;

import com.tokenrealty.settlement.dto.SettlementDtos.SettlementRetryResponse;
import com.tokenrealty.settlement.dto.SettlementDtos.SettlementSagaView;
import com.tokenrealty.settlement.service.SettlementSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementSagaService sagaService;

    @GetMapping("/{orderId}")
    public SettlementSagaView getSettlement(@PathVariable UUID orderId) {
        return sagaService.getByOrderId(orderId);
    }

    @PostMapping("/{orderId}/retry")
    public SettlementRetryResponse retry(@PathVariable UUID orderId) {
        return sagaService.retry(orderId);
    }
}
