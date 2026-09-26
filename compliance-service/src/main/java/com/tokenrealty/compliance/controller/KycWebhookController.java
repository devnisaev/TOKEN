package com.tokenrealty.compliance.controller;

import com.tokenrealty.compliance.dto.ComplianceDtos.KycWebhookPayload;
import com.tokenrealty.compliance.service.KycWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/compliance/webhooks/kyc")
@RequiredArgsConstructor
public class KycWebhookController {

    private final KycWebhookService kycWebhookService;

    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleKycWebhook(@PathVariable String provider,
                                 @Valid @RequestBody KycWebhookPayload payload) {
        kycWebhookService.handleWebhook(provider, payload);
    }
}
