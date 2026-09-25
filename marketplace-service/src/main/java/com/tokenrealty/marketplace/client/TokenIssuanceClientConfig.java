package com.tokenrealty.marketplace.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TokenIssuanceClientConfig {

    @Bean("tokenIssuanceRestClient")
    RestClient tokenIssuanceRestClient(
            @Value("${services.token-issuance.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
