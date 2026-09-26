package com.tokenrealty.document.controller;

import com.tokenrealty.document.service.StorageWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/v1/documents/webhooks")
@RequiredArgsConstructor
public class StorageWebhookController {

    private final StorageWebhookService storageWebhookService;

    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleStorageWebhook(
            @PathVariable String provider,
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Payload-Digest", required = false) String payloadDigest,
            @RequestHeader(value = "X-Signature", required = false) String signature)
            throws IOException {
        storageWebhookService.handleWebhook(provider, rawBody, payloadDigest, signature);
    }
}
