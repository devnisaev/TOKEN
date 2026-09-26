package com.tokenrealty.wallet.config;

import com.tokenrealty.security.ServiceTokenProvider;
import com.tokenrealty.security.client.ServiceRestClientBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientConfig {

    @Bean("paymentRestClient")
    RestClient paymentRestClient(
            @Value("${services.payment.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider
    ) {
        return ServiceRestClientBuilder.build(baseUrl, serviceTokenProvider);
    }

    @Bean("issuanceRestClient")
    RestClient issuanceRestClient(
            @Value("${services.token-issuance.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider
    ) {
        return ServiceRestClientBuilder.build(baseUrl, serviceTokenProvider);
    }
}
