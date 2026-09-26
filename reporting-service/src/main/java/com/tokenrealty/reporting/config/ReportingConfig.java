package com.tokenrealty.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReportingConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
