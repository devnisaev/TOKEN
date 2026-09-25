package com.tokenrealty.issuance.client;

import com.tokenrealty.security.ServiceTokenProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class MarketplaceClientConfig {

    @Bean("marketplaceRestClient")
    RestClient marketplaceRestClient(
            @Value("${services.marketplace.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider
    ) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        serviceTokenProvider.ifAvailable(provider -> builder.requestInterceptor((request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getAccessToken());
            return execution.execute(request, body);
        }));
        return builder.build();
    }
}
