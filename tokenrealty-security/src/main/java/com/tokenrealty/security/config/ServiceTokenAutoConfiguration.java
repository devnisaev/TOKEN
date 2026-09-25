package com.tokenrealty.security.config;

import com.tokenrealty.security.ServiceAccountProperties;
import com.tokenrealty.security.ServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@EnableConfigurationProperties(ServiceAccountProperties.class)
@ConditionalOnProperty(prefix = "tokenrealty.service-account", name = "client-id")
public class ServiceTokenAutoConfiguration {

    @Bean("authServiceRestClient")
    RestClient authServiceRestClient(@Value("${services.auth.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    ServiceTokenProvider serviceTokenProvider(
            @org.springframework.beans.factory.annotation.Qualifier("authServiceRestClient") RestClient authServiceRestClient,
            ServiceAccountProperties properties
    ) {
        return new ServiceTokenProvider(authServiceRestClient, properties);
    }
}
