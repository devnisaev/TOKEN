package com.tokenrealty.payment.controller;

import com.tokenrealty.payment.service.PaymentWebhookService;
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
@RequestMapping("/v1/payments/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handlePaymentWebhook(
            @PathVariable String provider,
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Payload-Digest", required = false) String payloadDigest,
            @RequestHeader(value = "X-Signature", required = false) String signature)
            throws IOException {
        paymentWebhookService.handleWebhook(provider, rawBody, payloadDigest, signature);
    }
}
