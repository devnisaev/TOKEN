package com.tokenrealty.integration.service;

import com.tokenrealty.integration.credential.CredentialEncryptionPort;
import com.tokenrealty.integration.dto.IntegrationDtos.IntegrationCredentialView;
import com.tokenrealty.integration.dto.IntegrationDtos.RotateCredentialRequest;
import com.tokenrealty.integration.entity.IntegrationCredential;
import com.tokenrealty.integration.entity.IntegrationType;
import com.tokenrealty.integration.repository.IntegrationCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IntegrationCredentialService {

    private final IntegrationCredentialRepository repository;
    private final CredentialEncryptionPort encryptionPort;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<IntegrationCredentialView> listCredentials(Pageable pageable) {
        return repository.findAll(pageable).map(this::toView);
    }

    @Transactional
    public IntegrationCredentialView rotateCredential(
            IntegrationType integrationType,
            String provider,
            RotateCredentialRequest request) {
        int nextVersion = repository.findFirstByIntegrationTypeAndProviderOrderByCredentialVersionDesc(
                        integrationType, provider)
                .map(existing -> existing.getCredentialVersion() + 1)
                .orElse(1);
        Instant rotatedAt = clock.instant();
        IntegrationCredential credential = repository.save(IntegrationCredential.builder()
                .integrationType(integrationType)
                .provider(provider)
                .secretCiphertext(encryptionPort.encrypt(request.secret()))
                .credentialVersion(nextVersion)
                .rotatedAt(rotatedAt)
                .build());
        return toView(credential);
    }

    private IntegrationCredentialView toView(IntegrationCredential credential) {
        return new IntegrationCredentialView(
                credential.getId(),
                credential.getIntegrationType(),
                credential.getProvider(),
                credential.getCredentialVersion(),
                credential.getRotatedAt(),
                credential.getCreatedAt()
        );
    }
}
