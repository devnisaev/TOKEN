package com.tokenrealty.integration.controller;

import com.tokenrealty.integration.service.WebhookRelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/integrations/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookRelayService webhookRelayService;

    @PostMapping("/kyc/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receiveKycWebhook(
            @PathVariable String provider,
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Payload-Digest", required = false) String payloadDigest,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        webhookRelayService.acceptKycWebhook(provider, rawBody, payloadDigest, signature);
    }

    @PostMapping("/storage/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receiveStorageWebhook(
            @PathVariable String provider,
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Payload-Digest", required = false) String payloadDigest,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        webhookRelayService.acceptDocumentWebhook(provider, rawBody, payloadDigest, signature);
    }
}
