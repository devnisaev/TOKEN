package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.entity.ProcessedEvent;
import com.tokenrealty.marketplace.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository repository;

    @Transactional
    public boolean tryClaim(UUID eventId, String eventType) {
        if (repository.existsByEventId(eventId)) {
            return false;
        }
        try {
            repository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
