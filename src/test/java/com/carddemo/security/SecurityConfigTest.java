package com.carddemo.security;

import com.carddemo.model.User;
import com.carddemo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Integration tests for {@link SecurityConfig}.
 *
 * <p>Verifies that the Spring Security rule set correctly replaces RACF resource profiles:
 * <ul>
 *   <li>{@code /api/auth/login} is publicly accessible (no token required).</li>
 *   <li>{@code /api/admin/**} requires {@code ROLE_ADMIN} (COBOL: CDEMO-USRTYP-ADMIN 'A').</li>
 *   <li>{@code /api/users/**} requires {@code ROLE_ADMIN}.</li>
 *   <li>{@code /api/**} requires any authenticated user.</li>
 *   <li>Unauthenticated requests to protected routes receive HTTP 401/403.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfig — RACF replacement integration tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // Seed admin user: COBOL type 'A' → ROLE_ADMIN
        userRepository.save(new User("ADMIN01 ", "Admin", "User",
                passwordEncoder.encode("pass1234"), "A"));
        // Seed regular user: COBOL type 'U' → ROLE_USER
        userRepository.save(new User("USER0001", "Regular", "User",
                passwordEncoder.encode("pass5678"), "U"));
    }

    // -------------------------------------------------------------------------
    // Public endpoints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login — permitAll (no token required)")
    void loginEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"USER0001\",\"password\":\"pass5678\"}"))
                // 404 is acceptable here — the controller isn't wired yet;
                // what matters is that Spring Security does NOT return 401/403.
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 && status != 403
                            : "Login endpoint should not be blocked by security, got " + status;
                });
    }

    // -------------------------------------------------------------------------
    // Admin-only endpoints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/admin/dashboard — ROLE_ADMIN is allowed")
    void adminEndpointAllowsAdminRole() throws Exception {
        UserDetails admin = buildAdmin();
        mockMvc.perform(get("/api/admin/dashboard").with(user(admin)))
                // 404 acceptable — controller not wired; 401/403 would indicate security failure
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 && status != 403
                            : "ROLE_ADMIN should access /api/admin/**, got " + status;
                });
    }

    @Test
    @DisplayName("GET /api/admin/dashboard — ROLE_USER is forbidden (403)")
    void adminEndpointForbidsUserRole() throws Exception {
        UserDetails regularUser = buildRegularUser();
        mockMvc.perform(get("/api/admin/dashboard").with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/dashboard — unauthenticated receives 401")
    void adminEndpointRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Users management endpoints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/users — ROLE_ADMIN is allowed")
    void usersEndpointAllowsAdminRole() throws Exception {
        UserDetails admin = buildAdmin();
        mockMvc.perform(get("/api/users").with(user(admin)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 && status != 403
                            : "ROLE_ADMIN should access /api/users/**, got " + status;
                });
    }

    @Test
    @DisplayName("GET /api/users — ROLE_USER is forbidden (403)")
    void usersEndpointForbidsUserRole() throws Exception {
        UserDetails regularUser = buildRegularUser();
        mockMvc.perform(get("/api/users").with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // General authenticated endpoint
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/cards — any authenticated user is allowed")
    void generalApiAllowsAuthenticatedUser() throws Exception {
        UserDetails regularUser = buildRegularUser();
        mockMvc.perform(get("/api/cards").with(user(regularUser)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 401 && status != 403
                            : "Authenticated user should reach /api/**, got " + status;
                });
    }

    @Test
    @DisplayName("GET /api/cards — unauthenticated receives 401")
    void generalApiRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserDetails buildAdmin() {
        return org.springframework.security.core.userdetails.User
                .withUsername("ADMIN01 ")
                .password(passwordEncoder.encode("pass1234"))
                .roles("ADMIN", "USER")
                .build();
    }

    private UserDetails buildRegularUser() {
        return org.springframework.security.core.userdetails.User
                .withUsername("USER0001")
                .password(passwordEncoder.encode("pass5678"))
                .roles("USER")
                .build();
    }
}
