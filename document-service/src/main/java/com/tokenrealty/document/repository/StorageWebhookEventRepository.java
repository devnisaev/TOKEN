package com.tokenrealty.document.repository;

import com.tokenrealty.document.entity.StorageWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StorageWebhookEventRepository extends JpaRepository<StorageWebhookEvent, UUID> {

    Optional<StorageWebhookEvent> findByPayloadDigest(String payloadDigest);
}
