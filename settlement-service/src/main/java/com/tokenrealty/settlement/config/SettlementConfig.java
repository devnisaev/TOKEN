package com.tokenrealty.settlement.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SettlementProperties.class)
public class SettlementConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
