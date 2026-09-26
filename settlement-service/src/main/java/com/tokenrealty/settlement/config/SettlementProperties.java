package com.tokenrealty.settlement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tokenrealty.settlement")
public record SettlementProperties(int stuckSlaMinutes) {
}
