package com.tokenrealty.registry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables @PreAuthorize in @WebMvcTest slices.
 * Imported into controller tests that need role enforcement.
 */
@Configuration
@EnableMethodSecurity
public class TestMethodSecurityConfig {
}
