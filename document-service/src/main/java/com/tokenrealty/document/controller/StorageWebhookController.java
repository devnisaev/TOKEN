package com.tokenrealty.document.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/documents/webhooks")
public class StorageWebhookController {

    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledgeStorageWebhook(
            @PathVariable String provider,
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Payload-Digest", required = false) String payloadDigest,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        // v1: acknowledge only — processing deferred to future release
    }
}
