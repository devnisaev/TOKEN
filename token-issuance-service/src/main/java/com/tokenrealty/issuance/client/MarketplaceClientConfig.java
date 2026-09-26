package com.tokenrealty.issuance.client;

import com.tokenrealty.security.ServiceTokenProvider;
import com.tokenrealty.security.client.ServiceRestClientBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MarketplaceClientConfig {

    @Bean("marketplaceRestClient")
    RestClient marketplaceRestClient(
            @Value("${services.marketplace.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider
    ) {
        return ServiceRestClientBuilder.build(baseUrl, serviceTokenProvider);
    }
}
