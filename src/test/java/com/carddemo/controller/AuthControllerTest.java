package com.carddemo.controller;

import com.carddemo.config.SecurityConfig;
import com.carddemo.model.LoginRequest;
import com.carddemo.model.LoginResponse;
import com.carddemo.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AuthController}.
 *
 * <p>These tests verify that the REST controller correctly maps the COSGN00C
 * COBOL sign-on business rules to HTTP responses:
 *
 * <pre>
 * COBOL scenario                              HTTP expectation
 * ──────────────────────────────────────────────────────────────
 * Valid user+password (ADMIN type)   → 200 OK + token
 * Valid user+password (USER type)    → 200 OK + token
 * Password mismatch (RESP-CD=0, pwd≠) → 401  "Wrong Password. Try again ..."
 * User not found   (RESP-CD=13)       → 401  "User not found. Try again ..."
 * System error     (RESP-CD=other)    → 401  "Unable to verify the User ..."
 * Blank username                      → 400  "Please enter User ID ..."
 * Blank password                      → 400  "Please enter Password ..."
 * </pre>
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController – COSGN00C migration tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    /**
     * Provide a no-op UserDetailsService so the SecurityConfig can build
     * its AuthenticationManager inside the lightweight @WebMvcTest context
     * (no real datasource is available).
     */
    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        public UserDetailsService testUserDetailsService() {
            return username -> {
                throw new UsernameNotFoundException(username);
            };
        }
    }

    // -------------------------------------------------------------------------
    // Happy-path: ADMIN user
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login – ADMIN user returns 200 with token and ROLE_ADMIN")
    void loginAdminUser_returns200WithToken() throws Exception {

        // "ADMUSER1" is 8 chars – within @Size(max=8)
        LoginRequest request  = new LoginRequest("ADMUSER1", "PASS1234");
        LoginResponse response = new LoginResponse(
                "mock-token-admin",
                "ADMUSER1",
                "ADMIN",
                List.of("ROLE_ADMIN"),
                AuthService.TRANID,
                AuthService.PROGRAM_NAME
        );

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token").value("mock-token-admin"))
               .andExpect(jsonPath("$.username").value("ADMUSER1"))
               .andExpect(jsonPath("$.userType").value("ADMIN"))
               .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
               .andExpect(jsonPath("$.transactionId").value("CC00"))
               .andExpect(jsonPath("$.programName").value("COSGN00C"));
    }

    // -------------------------------------------------------------------------
    // Happy-path: regular USER
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login – regular user returns 200 with token and ROLE_USER")
    void loginRegularUser_returns200WithToken() throws Exception {

        LoginRequest request  = new LoginRequest("JOHN    ", "MYPASS  ");
        LoginResponse response = new LoginResponse(
                "mock-token-user",
                "JOHN    ",
                "USER",
                List.of("ROLE_USER"),
                AuthService.TRANID,
                AuthService.PROGRAM_NAME
        );

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userType").value("USER"))
               .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    // -------------------------------------------------------------------------
    // Wrong password → 401 with COBOL error text
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login – wrong password returns 401 with COBOL error message")
    void loginWrongPassword_returns401() throws Exception {

        LoginRequest request = new LoginRequest("SOMEUSER", "BADPASS ");

        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException(AuthService.MSG_WRONG_PASSWORD));

        mockMvc.perform(post("/api/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.error").value("Wrong Password. Try again ..."));
    }

    // -------------------------------------------------------------------------
    // User not found (COBOL RESP-CD 13) → 401 with COBOL error text
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login – unknown user returns 401 with COBOL error message")
    void loginUserNotFound_returns401() throws Exception {

        LoginRequest request = new LoginRequest("NOBODY  ", "ANYPASS ");

        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new UsernameNotFoundException(AuthService.MSG_USER_NOT_FOUND));

        mockMvc.perform(post("/api/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.error").value("User not found. Try again ..."));
    }

    // -------------------------------------------------------------------------
    // System error (COBOL RESP-CD OTHER) → 401 with COBOL error text
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login – system error returns 401 'Unable to verify the User ...'")
    void loginSystemError_returns401UnableToVerify() throws Exception {

        LoginRequest request = new LoginRequest("ANYUSER ", "ANYPASS ");

        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException(AuthService.MSG_UNABLE_TO_VERIFY));

        mockMvc.perform(post("/api/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.error").value("Unable to verify the User ..."));
    }

    // -------------------------------------------------------------------------
    // Blank username → 400 (mirrors COBOL "Please enter User ID ...")
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login – blank username returns 400")
    void loginBlankUsername_returns400() throws Exception {

        LoginRequest request = new LoginRequest("", "PASS1234");

        mockMvc.perform(post("/api/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Blank password → 400 (mirrors COBOL "Please enter Password ...")
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login – blank password returns 400")
    void loginBlankPassword_returns400() throws Exception {

        LoginRequest request = new LoginRequest("SOMEUSER", "");

        mockMvc.perform(post("/api/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Null body → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login – null body returns 400")
    void loginNullBody_returns400() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("{}"))
               .andExpect(status().isBadRequest());
    }
}
