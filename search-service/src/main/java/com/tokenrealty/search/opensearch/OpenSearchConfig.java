package com.tokenrealty.search.opensearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "tokenrealty.search.backend", havingValue = "opensearch")
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {

    @Bean
    RestClient openSearchRestClient(OpenSearchProperties properties) {
        return RestClient.builder().baseUrl(properties.url()).build();
    }
}
