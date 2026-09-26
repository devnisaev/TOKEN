package com.tokenrealty.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PaymentProperties.class,
        PaymentAutoConfirmProperties.class,
        PaymentBlockchainProperties.class
})
public class AppConfig {
}
