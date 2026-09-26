package com.tokenrealty.rental.kafka.outbox;

import com.tokenrealty.outbox.OutboxStatus;
import com.tokenrealty.outbox.relay.OutboxRelayTarget;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OutboxEvent implements OutboxRelayTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Override
    public void markPublished(Instant publishedAt) {
        status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    @Override
    public void markFailed() {
        status = OutboxStatus.FAILED;
    }
}
