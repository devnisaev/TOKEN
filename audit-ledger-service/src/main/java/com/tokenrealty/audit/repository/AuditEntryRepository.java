package com.tokenrealty.audit.repository;

import com.tokenrealty.audit.entity.AuditEntry;
import com.tokenrealty.audit.entity.AuditSubjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {

    Page<AuditEntry> findBySubjectTypeAndSubjectIdOrderByOccurredAtDesc(
            AuditSubjectType subjectType, UUID subjectId, Pageable pageable);
}
