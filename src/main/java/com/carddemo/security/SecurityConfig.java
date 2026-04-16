package com.carddemo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration replacing RACF-based security from the COBOL/CICS CardDemo
 * application.
 *
 * <p>RACF → Spring Security mapping (verified from COBOL source):
 * <ul>
 *   <li>COBOL COSGN00C.cbl: RACROUTE DATASET (USRSEC) — replaced by JWT-protected REST endpoints.
 *   <li>COCOM01Y.cpy: CDEMO-USRTYP-ADMIN VALUE 'A' → {@code ROLE_ADMIN}</li>
 *   <li>COCOM01Y.cpy: CDEMO-USRTYP-USER  VALUE 'U' → {@code ROLE_USER}</li>
 *   <li>CSUSR01Y.cpy: SEC-USR-PWD verified at login → BCrypt-hashed password check</li>
 * </ul>
 *
 * <p>Endpoint authorization rules:
 * <ul>
 *   <li>{@code /api/auth/login}  — public (anonymous, returns JWT)
 *   <li>{@code /api/admin/**}    — requires {@code ROLE_ADMIN}
 *   <li>{@code /api/users/**}    — requires {@code ROLE_ADMIN}
 *   <li>{@code /api/**}          — requires any authenticated user
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CardDemoUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(CardDemoUserDetailsService userDetailsService,
                          JwtAuthFilter jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Main security filter chain.
     *
     * <p>Session management is STATELESS — JWT is the sole bearer of identity,
     * mirroring how RACF used an in-memory ACEE structure per CICS task.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless REST API
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless sessions — no JSESSIONID, each request carries a JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Return HTTP 401 for unauthenticated access to protected resources
            // (Spring Security 6 default is 403 for anonymous — we override for REST API)
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            // Authorization rules — mirrors RACF resource profiles in COSGN00C / COADM01C
            .authorizeHttpRequests(auth -> auth
                // Public: login endpoint — no prior authentication required
                .requestMatchers("/api/auth/login").permitAll()
                // Admin-only: user-management routes (mirrors COADM01C RACF ADMIN check)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Admin-only: user CRUD routes (mirrors COUSR01C/02C/03C admin-only paths)
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // Any remaining /api/** requires at least a valid authenticated user
                .requestMatchers("/api/**").authenticated()
                // Everything else (actuator, static) is open
                .anyRequest().permitAll()
            )

            // Wire up the custom DaoAuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // Insert JWT filter before Spring's own username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider backed by {@link CardDemoUserDetailsService}.
     * Passwords are verified using BCrypt (see {@link #passwordEncoder()}).
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} for use in the login controller.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder.
     *
     * <p>Replaces plain-text SEC-USR-PWD field (PIC X(08) in CSUSR01Y.cpy).
     * Passwords must be BCrypt-hashed before storage via the migration script.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
