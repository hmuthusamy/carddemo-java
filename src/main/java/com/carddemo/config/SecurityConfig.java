package com.carddemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig – Spring Security configuration replacing the COSGN00C CICS
 * security model.
 *
 * <p>COBOL COSGN00C was a CICS transaction that provided its own form-based
 * authentication against the USRSEC VSAM file. Spring Security centralises that
 * responsibility here, delegating credential verification to
 * {@link AuthenticationManager} (wired through {@link AuthService}).
 *
 * <p>Key mappings from the COBOL program:
 * <ul>
 *   <li>USRSEC VSAM read + password comparison → {@link AuthenticationManager}</li>
 *   <li>SEC-USR-TYPE "ADMIN" → {@code ROLE_ADMIN} → permits admin endpoints</li>
 *   <li>SEC-USR-TYPE "USER"  → {@code ROLE_USER}  → permits user endpoints</li>
 *   <li>{@code /api/auth/login} is public (mirrors the unauthenticated sign-on screen)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the HTTP security filter chain.
     *
     * <ul>
     *   <li>{@code /api/auth/**} is fully open – equivalent to the CICS sign-on
     *       transaction being reachable before any RACF check.</li>
     *   <li>All other paths require authentication.</li>
     *   <li>Session is STATELESS – JWT / token replaces the CICS COMMAREA context.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())          // REST API – no CSRF needed
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: sign-on endpoint (mirrors the unauthenticated COSGN00C screen)
                .requestMatchers("/api/auth/**").permitAll()
                // Admin screens (previously XCTL to COADM01C)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // All other requests require a valid session token
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Exposes the {@link AuthenticationManager} bean so that {@link com.carddemo.service.AuthService}
     * can call it directly – equivalent to COSGN00C performing the USRSEC file read.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Password encoder – BCrypt is the standard replacement for plain-text
     * passwords stored in the legacy USRSEC VSAM file (SEC-USR-PWD PIC X(08)).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
