package com.carddemo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtTokenProvider}.
 *
 * <p>Validates the full JWT lifecycle that replaces RACF ACEE token management:
 * <ul>
 *   <li>Token generation for both COBOL role types ('A'→ADMIN, 'U'→USER)</li>
 *   <li>Successful validation of a freshly issued token</li>
 *   <li>Username round-trip (SEC-USR-ID preserved in {@code sub} claim)</li>
 *   <li>Roles round-trip (COBOL SEC-USR-TYPE preserved in {@code roles} claim)</li>
 *   <li>Rejection of tampered tokens</li>
 *   <li>Rejection of expired tokens</li>
 * </ul>
 */
@DisplayName("JwtTokenProvider — RACF ACEE replacement unit tests")
class JwtTokenProviderTest {

    /**
     * Valid Base64-encoded 256-bit key used only in unit tests.
     * Never use this value in production — set JWT_SECRET env var instead.
     */
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW11c3QtYmUtbG9uZw==";

    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, ONE_HOUR_MS);
    }

    // -------------------------------------------------------------------------
    // generateToken
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateToken — returns non-null, non-empty JWT string")
    void generateToken_returnsNonEmptyString() {
        UserDetails user = buildUser("USER0001", "ROLE_USER");
        String token = tokenProvider.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken — JWT has three dot-separated parts (header.payload.signature)")
    void generateToken_hasThreeParts() {
        UserDetails user = buildUser("USER0001", "ROLE_USER");
        String token = tokenProvider.generateToken(user);
        assertThat(token.split("\\.")).hasSize(3);
    }

    // -------------------------------------------------------------------------
    // validateToken
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateToken — valid token returns true")
    void validateToken_validToken_returnsTrue() {
        UserDetails user = buildUser("ADMIN01 ", "ROLE_ADMIN");
        String token = tokenProvider.generateToken(user);
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken — tampered token returns false")
    void validateToken_tamperedToken_returnsFalse() {
        UserDetails user = buildUser("USER0001", "ROLE_USER");
        String token = tokenProvider.generateToken(user);
        // Flip the last character to corrupt the signature
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertThat(tokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken — empty string returns false")
    void validateToken_emptyString_returnsFalse() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken — null-like garbage string returns false")
    void validateToken_garbageString_returnsFalse() {
        assertThat(tokenProvider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken — expired token (1 ms TTL) returns false")
    void validateToken_expiredToken_returnsFalse() throws InterruptedException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(TEST_SECRET, 1L); // 1 ms
        UserDetails user = buildUser("USER0001", "ROLE_USER");
        String token = shortLivedProvider.generateToken(user);
        Thread.sleep(50); // let it expire
        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    // -------------------------------------------------------------------------
    // getUsername
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUsername — returns correct SEC-USR-ID for admin user")
    void getUsername_adminUser_returnsCorrectId() {
        String expectedId = "ADMIN01 ";
        UserDetails user = buildUser(expectedId, "ROLE_ADMIN");
        String token = tokenProvider.generateToken(user);
        assertThat(tokenProvider.getUsername(token)).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("getUsername — returns correct SEC-USR-ID for regular user")
    void getUsername_regularUser_returnsCorrectId() {
        String expectedId = "USER0001";
        UserDetails user = buildUser(expectedId, "ROLE_USER");
        String token = tokenProvider.generateToken(user);
        assertThat(tokenProvider.getUsername(token)).isEqualTo(expectedId);
    }

    // -------------------------------------------------------------------------
    // getRoles
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getRoles — COBOL type 'A' admin token contains ROLE_ADMIN and ROLE_USER")
    void getRoles_adminToken_containsBothRoles() {
        // Admin users (COBOL 'A') receive ROLE_ADMIN + ROLE_USER
        UserDetails admin = new User("ADMIN01 ", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_USER")));
        String token = tokenProvider.generateToken(admin);
        List<String> roles = tokenProvider.getRoles(token);
        assertThat(roles).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("getRoles — COBOL type 'U' user token contains only ROLE_USER")
    void getRoles_userToken_containsOnlyRoleUser() {
        UserDetails regularUser = new User("USER0001", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = tokenProvider.generateToken(regularUser);
        List<String> roles = tokenProvider.getRoles(token);
        assertThat(roles).containsExactly("ROLE_USER");
        assertThat(roles).doesNotContain("ROLE_ADMIN");
    }

    // -------------------------------------------------------------------------
    // Role mapping (CardDemoUserDetailsService.mapRoles)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("mapRoles — COBOL 'A' maps to ROLE_ADMIN + ROLE_USER")
    void mapRoles_cobolAdmin_returnsAdminAndUser() {
        var authorities = CardDemoUserDetailsService.mapRoles("A");
        assertThat(authorities).extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("mapRoles — COBOL 'U' maps to ROLE_USER only")
    void mapRoles_cobolUser_returnsUserOnly() {
        var authorities = CardDemoUserDetailsService.mapRoles("U");
        assertThat(authorities).extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("mapRoles — unknown type defaults to ROLE_USER (principle of least privilege)")
    void mapRoles_unknownType_defaultsToRoleUser() {
        var authorities = CardDemoUserDetailsService.mapRoles("X");
        assertThat(authorities).extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("mapRoles — null type defaults to ROLE_USER")
    void mapRoles_nullType_defaultsToRoleUser() {
        var authorities = CardDemoUserDetailsService.mapRoles(null);
        assertThat(authorities).extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserDetails buildUser(String username, String role) {
        return new User(username, "irrelevant-password",
                List.of(new SimpleGrantedAuthority(role)));
    }
}
