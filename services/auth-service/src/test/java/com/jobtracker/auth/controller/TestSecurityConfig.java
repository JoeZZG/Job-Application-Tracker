package com.jobtracker.auth.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/*
 * Permissive security configuration used only during @WebMvcTest slice tests.
 *
 * Disables CSRF and permits all requests so that web-layer logic (validation,
 * serialisation, status codes) can be verified in isolation without the real
 * JWT filter chain interfering. Authentication state for individual tests is
 * supplied directly via SecurityMockMvcRequestPostProcessors.authentication().
 */
@TestConfiguration
class TestSecurityConfig {

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
