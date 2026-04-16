package com.carddemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Spring Security configuration stub.
 *
 * <p>Migrated from the COBOL concept of CDEMO-USRTYP-USER / CDEMO-USRTYP-ADMIN
 * in COCOM01Y.cpy.  The original COMEN01C checked user type at runtime via
 * CDEMO-MENU-OPT-USRTYPE to guard admin-only menu options.
 *
 * <p>Here this is expressed via Spring Security roles:
 * <ul>
 *   <li>{@code ROLE_USER}  — regular CardDemo user (CDEMO-USRTYP-USER = 'U')</li>
 *   <li>{@code ROLE_ADMIN} — administrator      (CDEMO-USRTYP-USER = 'A')</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails regularUser = User.builder()
                .username("user")
                .password(encoder.encode("user-pass"))
                .roles("USER")                       // CDEMO-USRTYP = 'U'
                .build();

        UserDetails adminUser = User.builder()
                .username("admin")
                .password(encoder.encode("admin-pass"))
                .roles("ADMIN", "USER")              // CDEMO-USRTYP = 'A'
                .build();

        return new InMemoryUserDetailsManager(regularUser, adminUser);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())            // disabled for REST API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/menu/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(basic -> {});                 // Basic auth for simplicity
        return http.build();
    }
}
