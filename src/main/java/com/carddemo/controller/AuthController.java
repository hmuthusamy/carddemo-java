package com.carddemo.controller;

import com.carddemo.model.LoginRequest;
import com.carddemo.model.LoginResponse;
import com.carddemo.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AuthController – REST entry-point that replaces the COSGN00C CICS sign-on program.
 *
 * <pre>
 * COBOL COSGN00C screen / commarea flow  →  REST equivalent
 * ─────────────────────────────────────────────────────────
 * CICS RECEIVE MAP(COSGN0A)              →  POST /api/auth/login
 * USERIDI / PASSWDI fields               →  @RequestBody LoginRequest
 * EXEC CICS READ DATASET(USRSEC)         →  AuthService.authenticate()
 * SEC-USR-TYPE → CDEMO-USER-TYPE         →  LoginResponse.userType / roles
 * EXEC CICS XCTL PROGRAM(COADM01C|…)    →  LoginResponse.userType drives UI redirect
 * EXEC CICS RETURN COMMAREA(…)           →  LoginResponse (token + metadata)
 *
 * Error messages (verbatim from COBOL):
 *   "Please enter User ID ..."          → 400 (Bean Validation)
 *   "Please enter Password ..."         → 400 (Bean Validation)
 *   "User not found. Try again ..."     → 401
 *   "Wrong Password. Try again ..."     → 401
 *   "Unable to verify the User ..."     → 401
 * </pre>
 *
 * <h3>Security note</h3>
 * This endpoint must be explicitly permitted in the Spring Security filter chain
 * ({@code SecurityConfig}) so it is accessible without a prior authentication token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a CardDemo user.
     *
     * <p>Equivalent to the COSGN00C {@code PROCESS-ENTER-KEY} → {@code READ-USER-SEC-FILE}
     * paragraph sequence.
     *
     * @param loginRequest {@code {username, password}} – mirrors USERIDI/PASSWDI map fields
     * @return 200 with {@link LoginResponse} on success, 401 on authentication failure,
     *         400 on validation failure
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {

        log.debug("Login attempt for user: {}", loginRequest.getUsername());

        try {
            LoginResponse response = authService.authenticate(loginRequest);
            return ResponseEntity.ok(response);

        } catch (UsernameNotFoundException ex) {
            // COBOL WHEN 13: 'User not found. Try again ...'
            log.warn("Login failed – user not found: {}", loginRequest.getUsername());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());

        } catch (BadCredentialsException ex) {
            // COBOL: 'Wrong Password. Try again ...' or 'Unable to verify the User ...'
            log.warn("Login failed – bad credentials for user: {}", loginRequest.getUsername());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());

        } catch (AuthenticationException ex) {
            // COBOL WHEN OTHER: 'Unable to verify the User ...'
            log.error("Login failed – authentication error for user {}: {}",
                      loginRequest.getUsername(), ex.getMessage());
            return buildErrorResponse(HttpStatus.UNAUTHORIZED,
                                      AuthService.MSG_UNABLE_TO_VERIFY);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a standardised error body: {@code {"error": "<message>"}}.
     * The message text is kept verbatim from the original COBOL WS-MESSAGE values
     * so that any downstream consumer can match legacy error strings.
     */
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status,
                                                                    String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
