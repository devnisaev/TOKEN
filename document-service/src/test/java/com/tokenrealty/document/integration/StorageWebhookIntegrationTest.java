package com.tokenrealty.document.integration;

import com.tokenrealty.document.repository.StorageWebhookEventRepository;
import com.tokenrealty.document.service.StorageWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StorageWebhookIntegrationTest {

    @Autowired StorageWebhookService storageWebhookService;
    @Autowired StorageWebhookEventRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("storage webhook persists pin event")
    void storageWebhook_persistsEvent() throws Exception {
        String body = "{\"event\":\"PINNED\",\"objectKey\":\"deeds/building-1.pdf\",\"ipfsCid\":\"QmExample\"}";

        storageWebhookService.handleWebhook("pinata", body, "digest-123", null);

        assertThat(repository.count()).isEqualTo(1);
        var event = repository.findAll().getFirst();
        assertThat(event.getProvider()).isEqualTo("pinata");
        assertThat(event.getEventType()).isEqualTo("PINNED");
        assertThat(event.getObjectKey()).isEqualTo("deeds/building-1.pdf");
        assertThat(event.getIpfsCid()).isEqualTo("QmExample");
        assertThat(event.getPayloadDigest()).isEqualTo("digest-123");
    }

    @Test
    @DisplayName("duplicate payload digest is deduped")
    void duplicatePayloadDigest_deduped() throws Exception {
        String body = "{\"event\":\"upload.completed\",\"objectKey\":\"lease-42.pdf\"}";

        storageWebhookService.handleWebhook("pinata", body, "same-digest", null);
        storageWebhookService.handleWebhook("pinata", body, "same-digest", null);

        assertThat(repository.count()).isEqualTo(1);
    }
}
