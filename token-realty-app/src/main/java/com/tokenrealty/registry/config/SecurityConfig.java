package com.tokenrealty.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security filter chain — safe to import in @WebMvcTest.
 * Does NOT define UserDetailsService or PasswordEncoder beans
 * (those are in SecurityUserConfig to avoid circular dependencies in test slices).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/v1/**")
                        .hasAnyRole("ADMIN", "PROPERTY_MANAGER", "APPRAISER", "COMPLIANCE")
                        .requestMatchers(HttpMethod.PUT, "/v1/**")
                        .hasAnyRole("ADMIN", "PROPERTY_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/v1/**")
                        .hasAnyRole("ADMIN", "PROPERTY_MANAGER", "APPRAISER", "COMPLIANCE")
                        .requestMatchers(HttpMethod.DELETE, "/v1/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.realmName("TokenRealty Property Registry"));

        return http.build();
    }
}