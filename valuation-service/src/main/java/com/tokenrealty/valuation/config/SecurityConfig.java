package com.tokenrealty.valuation.config;

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
                        .requestMatchers(HttpMethod.POST, "/v1/valuations").hasAnyRole("APPRAISER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/valuations/schedules").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/valuations/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/valuations/*/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/valuations/**")
                        .hasAnyRole("ADMIN", "PROPERTY_MANAGER", "APPRAISER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
