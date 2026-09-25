package com.tokenrealty.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * User/password beans — kept separate from SecurityConfig so that
 * @WebMvcTest slices can import SecurityConfig alone without needing
 * PasswordEncoder and UserDetailsService in the test application context.
 */
@Configuration
public class SecurityUserConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin")
                        .password(encoder.encode("admin123")).roles("ADMIN").build(),
                User.withUsername("manager")
                        .password(encoder.encode("manager123")).roles("PROPERTY_MANAGER").build(),
                User.withUsername("appraiser")
                        .password(encoder.encode("appraiser123")).roles("APPRAISER").build(),
                User.withUsername("compliance")
                        .password(encoder.encode("compliance123")).roles("COMPLIANCE").build(),
                User.withUsername("investor")
                        .password(encoder.encode("investor123")).roles("INVESTOR").build()
        );
    }
}