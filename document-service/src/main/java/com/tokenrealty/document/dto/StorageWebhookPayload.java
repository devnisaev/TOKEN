package com.tokenrealty.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StorageWebhookPayload(
        @NotBlank @Size(max = 100) String event,
        @Size(max = 500) String objectKey,
        @Size(max = 200) String ipfsCid
) {
}
