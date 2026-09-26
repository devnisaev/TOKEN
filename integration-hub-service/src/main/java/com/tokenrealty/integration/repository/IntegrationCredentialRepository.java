package com.tokenrealty.integration.repository;

import com.tokenrealty.integration.entity.IntegrationCredential;
import com.tokenrealty.integration.entity.IntegrationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IntegrationCredentialRepository extends JpaRepository<IntegrationCredential, UUID> {

    Optional<IntegrationCredential> findFirstByIntegrationTypeAndProviderOrderByCredentialVersionDesc(
            IntegrationType integrationType,
            String provider);
}
