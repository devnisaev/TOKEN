package com.tokenrealty.kafka.config;

import com.tokenrealty.kafka.idempotency.ProcessedEvent;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@AutoConfigurationPackage(basePackageClasses = ProcessedEvent.class)
@ComponentScan(basePackages = "com.tokenrealty.kafka")
public class TokenRealtyKafkaAutoConfiguration {
}
