package com.tokenrealty.governance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class GovernanceConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
