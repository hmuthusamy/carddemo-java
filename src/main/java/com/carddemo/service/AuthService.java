package com.carddemo.service;

import com.carddemo.model.LoginRequest;
import com.carddemo.model.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AuthService – Spring Security equivalent of the COSGN00C COBOL sign-on logic.
 *
 * <pre>
 * COBOL equivalent flow (READ-USER-SEC-FILE paragraph):
 *
 *   EXEC CICS READ DATASET(USRSEC) ...           → UserDetailsService.loadUserByUsername()
 *   WHEN 0  → record found
 *     IF SEC-USR-PWD = WS-USER-PWD               → AuthenticationManager.authenticate()
 *       MOVE SEC-USR-TYPE TO CDEMO-USER-TYPE      → roles derived from user type
 *       XCTL to COADM01C / COMEN01C               → redirect URL in LoginResponse
 *     ELSE → 'Wrong Password. Try again ...'      → BadCredentialsException
 *   WHEN 13 → 'User not found. Try again ...'     → UsernameNotFoundException
 *   WHEN OTHER → 'Unable to verify the User ...'  → AuthenticationException
 * </pre>
 *
 * <p>RACF role mapping:
 * <ul>
 *   <li>SEC-USR-TYPE = "ADMIN" → {@code ROLE_ADMIN} (previously XCTL to COADM01C)</li>
 *   <li>SEC-USR-TYPE = "USER"  → {@code ROLE_USER}  (previously XCTL to COMEN01C)</li>
 * </ul>
 *
 * <p>Token strategy: For this migration the token is a signed Base64-encoded
 * opaque token.  A production implementation should replace
 * {@link #generateToken} with a proper JWT library (e.g., JJWT / Nimbus).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Mirror of WS-TRANID and WS-PGMNAME working-storage fields from COSGN00C
    public static final String TRANID       = "CC00";
    public static final String PROGRAM_NAME = "COSGN00C";

    // COBOL error messages – preserved verbatim for backward compatibility
    public static final String MSG_WRONG_PASSWORD   = "Wrong Password. Try again ...";
    public static final String MSG_USER_NOT_FOUND   = "User not found. Try again ...";
    public static final String MSG_UNABLE_TO_VERIFY = "Unable to verify the User ...";

    private final AuthenticationManager authenticationManager;

    public AuthService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user and returns a {@link LoginResponse} containing a
     * session token and the Spring Security roles derived from the USRSEC record.
     *
     * <p>Username is upper-cased before authentication, mirroring the COBOL:
     * {@code MOVE FUNCTION UPPER-CASE(USERIDI OF COSGN0AI) TO WS-USER-ID}
     *
     * @param request the login request containing username and password
     * @return a populated {@link LoginResponse} on success
     * @throws AuthenticationException (subclasses) on failure – caller maps to HTTP 401
     */
    public LoginResponse authenticate(LoginRequest request) {

        // COBOL: MOVE FUNCTION UPPER-CASE(USERIDI) TO WS-USER-ID
        String upperUsername = request.getUsername().toUpperCase();
        String password      = request.getPassword();

        log.debug("Authenticating user: {}", upperUsername);

        // Delegate to Spring Security – equivalent to EXEC CICS READ DATASET(USRSEC)
        // followed by password comparison. Exceptions map 1-to-1 to COBOL RESP codes.
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(upperUsername, password));
        } catch (UsernameNotFoundException ex) {
            // COBOL WHEN 13: 'User not found. Try again ...'
            log.warn("User not found: {}", upperUsername);
            throw new UsernameNotFoundException(MSG_USER_NOT_FOUND, ex);
        } catch (BadCredentialsException ex) {
            // COBOL: 'Wrong Password. Try again ...'
            log.warn("Bad credentials for user: {}", upperUsername);
            throw new BadCredentialsException(MSG_WRONG_PASSWORD, ex);
        } catch (AuthenticationException ex) {
            // COBOL WHEN OTHER: 'Unable to verify the User ...'
            log.error("Authentication failure for user {}: {}", upperUsername, ex.getMessage());
            throw new BadCredentialsException(MSG_UNABLE_TO_VERIFY, ex);
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // Map Spring Security roles → user type (mirrors SEC-USR-TYPE in USRSEC)
        String userType = resolveUserType(authorities);

        // Roles as string list for JSON response
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Generate opaque session token (replace with JWT in production)
        String token = generateToken(upperUsername, roles);

        log.info("User '{}' authenticated successfully with roles: {}", upperUsername, roles);

        return new LoginResponse(token, upperUsername, userType, roles, TRANID, PROGRAM_NAME);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Derives the legacy USRSEC user-type string from Spring GrantedAuthority list.
     *
     * <ul>
     *   <li>{@code ROLE_ADMIN} → "ADMIN" (maps to CDEMO-USRTYP-ADMIN, XCTL COADM01C)</li>
     *   <li>anything else     → "USER"  (maps to COMEN01C)</li>
     * </ul>
     */
    String resolveUserType(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals) ? "ADMIN" : "USER";
    }

    /**
     * Generates a short-lived opaque session token.
     *
     * <p>Format: {@code Base64(username:UUID)} – intentionally simple for the
     * migration stub.  Replace with a proper JWT (HS256/RS256) for production.
     */
    String generateToken(String username, List<String> roles) {
        String payload = username + ":" + UUID.randomUUID();
        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
