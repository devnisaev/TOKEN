package com.tokenrealty.kafka.idempotency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessedEventClaimService {

    private final EntityManager entityManager;

    @Transactional
    public boolean tryClaim(UUID eventId, String eventType) {
        Long existing = entityManager.createQuery(
                        "SELECT COUNT(p) FROM ProcessedEvent p WHERE p.eventId = :eventId", Long.class)
                .setParameter("eventId", eventId)
                .getSingleResult();
        if (existing > 0) {
            return false;
        }
        try {
            entityManager.persist(new ProcessedEvent(eventId, eventType, Instant.now()));
            entityManager.flush();
            return true;
        } catch (PersistenceException ex) {
            return false;
        }
    }
}
