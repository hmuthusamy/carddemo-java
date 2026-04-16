package com.carddemo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — executes once per HTTP request.
 *
 * <p>This filter replaces the RACF ACEE (Accessor Environment Element) verification
 * that was performed on every CICS task in the COBOL CardDemo application. In the
 * original system, COSGN00C.cbl called RACROUTE to authenticate users against the
 * USRSEC VSAM file; each subsequent CICS program relied on the ACEE being set in the
 * task control block. Here, the JWT carries the equivalent identity and roles.
 *
 * <p>Processing flow:
 * <ol>
 *   <li>Extract {@code Bearer <token>} from the {@code Authorization} header.</li>
 *   <li>Validate the JWT signature and expiry via {@link JwtTokenProvider}.</li>
 *   <li>Load {@link UserDetails} (including RACF-derived roles) from
 *       {@link CardDemoUserDetailsService}.</li>
 *   <li>Populate the {@link SecurityContextHolder} so that subsequent filters and
 *       controllers can access the authenticated principal.</li>
 * </ol>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CardDemoUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider,
                         CardDemoUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Core filter logic: extract → validate → authenticate.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String token = extractToken(request);

        if (token == null) {
            // No JWT present — continue as anonymous (Spring Security handles rejection
            // for protected routes further down the filter chain)
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid or expired JWT received from {}", request.getRemoteAddr());
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if no authentication is already set for this request
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            final String username = jwtTokenProvider.getUsername(token);
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user '{}' with roles {}", username,
                        userDetails.getAuthorities());
            } catch (UsernameNotFoundException ex) {
                log.warn("JWT references unknown user '{}': {}", username, ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT string from the {@code Authorization: Bearer <token>} header.
     *
     * @param request the incoming HTTP request
     * @return the JWT string, or {@code null} if the header is absent / malformed
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
