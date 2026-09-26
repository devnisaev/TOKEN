package com.tokenrealty.document.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "storage_webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageWebhookEvent extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "ipfs_cid", length = 200)
    private String ipfsCid;

    @Column(name = "payload_digest", unique = true, length = 128)
    private String payloadDigest;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
