package com.tokenrealty.integration.controller;

import com.tokenrealty.integration.dto.IntegrationDtos.IntegrationDeliveryView;
import com.tokenrealty.integration.service.WebhookRelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/integrations/deliveries")
@RequiredArgsConstructor
public class IntegrationDeliveryController {

    private final WebhookRelayService webhookRelayService;

    @GetMapping
    public Page<IntegrationDeliveryView> list(@PageableDefault(size = 20) Pageable pageable) {
        return webhookRelayService.listDeliveries(pageable);
    }
}
