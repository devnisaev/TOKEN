package com.tokenrealty.indexer.repository;

import com.tokenrealty.indexer.entity.IndexerCursor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexerCursorRepository extends JpaRepository<IndexerCursor, String> {
}
