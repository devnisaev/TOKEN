package com.tokenrealty.integration.controller;

import com.tokenrealty.integration.dto.IntegrationDtos.IntegrationCredentialView;
import com.tokenrealty.integration.dto.IntegrationDtos.RotateCredentialRequest;
import com.tokenrealty.integration.entity.IntegrationType;
import com.tokenrealty.integration.service.IntegrationCredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/integrations/credentials")
@RequiredArgsConstructor
public class IntegrationCredentialController {

    private final IntegrationCredentialService credentialService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<IntegrationCredentialView> listCredentials(@PageableDefault(size = 20) Pageable pageable) {
        return credentialService.listCredentials(pageable);
    }

    @PostMapping("/{integrationType}/{provider}/rotate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public IntegrationCredentialView rotateCredential(
            @PathVariable IntegrationType integrationType,
            @PathVariable String provider,
            @Valid @RequestBody RotateCredentialRequest request) {
        return credentialService.rotateCredential(integrationType, provider, request);
    }
}
