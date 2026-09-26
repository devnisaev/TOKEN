package com.tokenrealty.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AuditConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
