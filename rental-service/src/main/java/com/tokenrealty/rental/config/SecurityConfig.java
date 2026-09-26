package com.tokenrealty.rental.config;

import com.tokenrealty.security.ActuatorSecurityPaths;
import com.tokenrealty.security.JwtAuthenticationFilter;
import com.tokenrealty.security.config.TokenRealtyJwtAutoConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(TokenRealtyJwtAutoConfiguration.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ActuatorSecurityPaths.PUBLIC).permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/v1/leases/**")
                        .hasAnyRole("ADMIN", "PROPERTY_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/v1/rent-payments")
                        .hasAnyRole("ADMIN", "PROPERTY_MANAGER", "TENANT")
                        .requestMatchers(HttpMethod.POST, "/v1/maintenance-tickets")
                        .hasAnyRole("ADMIN", "PROPERTY_MANAGER", "TENANT")
                        .requestMatchers(HttpMethod.POST, "/v1/**")
                        .hasAnyRole("ADMIN", "PROPERTY_MANAGER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
