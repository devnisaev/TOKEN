package com.tokenrealty.corporateactions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CorporateActionsConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
