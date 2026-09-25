package com.tokenrealty.registry.repository;

import com.tokenrealty.registry.entity.PropertyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyDocumentRepository extends JpaRepository<PropertyDocument, UUID> {

    List<PropertyDocument> findByBuildingId(UUID buildingId);

    List<PropertyDocument> findByFlatId(UUID flatId);

    List<PropertyDocument> findByBuildingIdAndDocumentType(
            UUID buildingId, PropertyDocument.DocumentType type);

    List<PropertyDocument> findByFlatIdAndDocumentType(
            UUID flatId, PropertyDocument.DocumentType type);
}
