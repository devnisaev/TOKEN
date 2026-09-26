package com.tokenrealty.security.client;

import com.tokenrealty.security.ServiceTokenProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

public final class ServiceRestClientBuilder {

    private ServiceRestClientBuilder() {
    }

    public static RestClient build(String baseUrl, ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        serviceTokenProvider.ifAvailable(provider -> builder.requestInterceptor((request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getAccessToken());
            return execution.execute(request, body);
        }));
        return builder.build();
    }
}
