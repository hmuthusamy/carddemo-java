package com.carddemo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT token provider — issues and validates HS256-signed JWTs.
 *
 * <p>This component replaces the RACF ACEE (Accessor Environment Element) used by the
 * COBOL CardDemo programs. Previously, COSGN00C.cbl performed a RACROUTE call to
 * authenticate the user and store their type (SEC-USR-TYPE, PIC X(01)) in CDEMO-USER-TYPE.
 * That per-task security context is now encoded as claims inside a stateless JWT.
 *
 * <p>Token claims:
 * <ul>
 *   <li>{@code sub}   — username (maps to SEC-USR-ID, PIC X(08))</li>
 *   <li>{@code roles} — list of Spring role strings derived from SEC-USR-TYPE</li>
 *   <li>{@code iat}   — issued-at timestamp</li>
 *   <li>{@code exp}   — expiry timestamp (default 24 h)</li>
 * </ul>
 *
 * <p>Configuration (application.yml):
 * <pre>
 * carddemo:
 *   jwt:
 *     secret: &lt;base64-encoded 256-bit key&gt;
 *     expiration-ms: 86400000
 * </pre>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String ROLES_CLAIM = "roles";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${carddemo.jwt.secret}") String base64Secret,
            @Value("${carddemo.jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT for the given {@link UserDetails}.
     *
     * <p>The {@code roles} claim carries the full list of granted authorities (e.g.
     * {@code ROLE_ADMIN}, {@code ROLE_USER}) so downstream services can make access
     * decisions without a DB lookup — analogous to the RACF ACEE being passed in the
     * CICS task control block.
     *
     * @param userDetails the authenticated principal
     * @return a signed, compact JWT string
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put(ROLES_CLAIM, roles);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the JWT signature and checks that the token has not expired.
     *
     * @param token the compact JWT string
     * @return {@code true} if valid and not expired; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            log.warn("JWT token is null or empty: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extracts the username (subject) from a valid JWT.
     *
     * <p>Maps to {@code SEC-USR-ID} (PIC X(08)) from the COBOL CSUSR01Y.cpy copybook.
     *
     * @param token a valid, signed JWT string
     * @return the username embedded in the token's {@code sub} claim
     */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the roles list claim from a valid JWT.
     *
     * @param token a valid, signed JWT string
     * @return list of role strings (e.g. {@code ["ROLE_ADMIN"]})
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parseClaims(token).get(ROLES_CLAIM);
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
