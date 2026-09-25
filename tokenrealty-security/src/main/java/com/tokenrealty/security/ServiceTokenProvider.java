package com.tokenrealty.security;

import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class ServiceTokenProvider {

    private static final long REFRESH_BUFFER_SECONDS = 60;

    private final RestClient authRestClient;
    private final ServiceAccountProperties properties;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public ServiceTokenProvider(RestClient authRestClient, ServiceAccountProperties properties) {
        this.authRestClient = authRestClient;
        this.properties = properties;
    }

    public String getAccessToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.isValid()) {
            return current.token();
        }
        return refreshToken();
    }

    private synchronized String refreshToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.isValid()) {
            return current.token();
        }

        ServiceTokenResponse response = authRestClient.post()
                .uri("/v1/auth/service-token")
                .body(new ServiceTokenRequest(properties.getClientId(), properties.getClientSecret()))
                .retrieve()
                .body(ServiceTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Auth service returned empty service token");
        }

        Instant expiresAt = Instant.now().plusSeconds(response.expiresInSeconds());
        cachedToken.set(new CachedToken(response.accessToken(), expiresAt));
        return response.accessToken();
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().plusSeconds(REFRESH_BUFFER_SECONDS).isBefore(expiresAt);
        }
    }
}
