package com.carddemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig – Spring Security configuration for the CardDemo application.
 *
 * <p>Replaces the COBOL CICS transaction-level security that restricted the
 * CU00 transaction (COUSR00C program) to admin-only terminal sessions.
 *
 * <p>All {@code /api/users/**} endpoints require ROLE_ADMIN,
 * enforced via {@code @PreAuthorize("hasRole('ADMIN')")} in
 * {@link com.carddemo.controller.UserManagementController}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * HTTP security filter chain.
     * Uses stateless JWT-compatible session management.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm ->
                    sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {}); // Basic auth for simplicity; swap for JWT in prod
        return http.build();
    }

    /**
     * BCrypt password encoder – replaces the COBOL plaintext SEC-USR-PWD PIC X(08).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
