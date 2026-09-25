package com.tokenrealty.issuance.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PropertyRegistryClientConfig {

    @Bean
    public RestClient propertyRegistryRestClient(
            @Value("${services.property-registry.url}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
