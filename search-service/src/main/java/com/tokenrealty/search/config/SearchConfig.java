package com.tokenrealty.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SearchConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
