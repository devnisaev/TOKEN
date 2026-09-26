package com.tokenrealty.wallet.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WalletSignRateLimitProperties.class)
public class WalletConfig {

    @Bean
    WalletSignRateLimitFilter walletSignRateLimitFilter(WalletSignRateLimitProperties properties) {
        return new WalletSignRateLimitFilter(properties);
    }
}
