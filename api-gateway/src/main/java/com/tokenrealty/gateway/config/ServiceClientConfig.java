package com.tokenrealty.gateway.config;

import com.tokenrealty.security.ServiceTokenProvider;
import com.tokenrealty.security.client.ServiceRestClientBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientConfig {

    @Bean("propertyRegistryRestClient")
    RestClient propertyRegistryRestClient(
            @Value("${services.property-registry.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
        return ServiceRestClientBuilder.build(baseUrl, serviceTokenProvider);
    }

    @Bean("marketplaceRestClient")
    RestClient marketplaceRestClient(
            @Value("${services.marketplace.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
        return ServiceRestClientBuilder.build(baseUrl, serviceTokenProvider);
    }

    @Bean("tokenIssuanceRestClient")
    RestClient tokenIssuanceRestClient(
            @Value("${services.token-issuance.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
        return ServiceRestClientBuilder.build(baseUrl, serviceTokenProvider);
    }
}
